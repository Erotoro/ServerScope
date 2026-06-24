package com.serverscope.bootstrap;

import com.serverscope.bootstrap.command.ServerScopeCommandExecutor;
import com.serverscope.bootstrap.config.BootstrapConfigLoader;
import com.serverscope.bootstrap.wiring.PluginRuntime;
import com.serverscope.bootstrap.wiring.PluginWiringFactory;
import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.ServerScopeConfig;
import com.serverscope.core.concurrent.NamedThreadFactory;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.runtime.DefaultRuntimeInfoService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class ServerScopePlugin extends JavaPlugin {
    private final Object runtimeLock = new Object();
    private final ExecutorService configExecutor = Executors.newSingleThreadExecutor(
            NamedThreadFactory.daemon("serverscope-config-loader"));
    private volatile PluginRuntime runtime;
    private volatile TranslationService translations;
    private volatile ServerScopeConfig activeConfig;

    @Override
    public void onEnable() {
        translations = new TranslationService(getLogger(), new LocalizationConfig("en", false));
        try {
            BootstrapConfigLoader configLoader = new BootstrapConfigLoader(this);
            configLoader.ensureDefaultConfigExists();
            replaceRuntime(configLoader.loadFromDisk(), false);
            registerCommands();
            getLogger().info(t("plugin.start.success"));
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, t("plugin.start.failure"), exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            PluginRuntime current = runtime;
            runtime = null;
            if (current != null) {
                if (current.runtimeInfoService() instanceof DefaultRuntimeInfoService runtimeInfoService) {
                    runtimeInfoService.markStopped();
                }
                current.lifecycleManager().stopAll();
            }
        } finally {
            configExecutor.shutdownNow();
            getLogger().info(t("plugin.stop.success"));
        }
    }

    private void registerCommands() {
        var command = getCommand("serverscope");
        if (command == null) {
            throw new IllegalStateException(t("plugin.command.not_defined"));
        }
        ServerScopeCommandExecutor executor = new ServerScopeCommandExecutor(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public PluginRuntime requireRuntime() {
        PluginRuntime current = runtime;
        if (current == null) {
            throw new IllegalStateException("ServerScope runtime is not initialized");
        }
        return current;
    }

    public void reloadServerScopeRuntime(CommandSender sender) {
        CompletableFuture.supplyAsync(() -> new BootstrapConfigLoader(this).loadFromDisk(), configExecutor)
                .whenComplete((config, throwable) -> runPlatformTask(() -> {
                    if (throwable != null) {
                        getLogger().log(Level.SEVERE, t("plugin.reload.failure.config"), throwable);
                        sender.sendMessage(Component.text(t("plugin.reload.message.failed_config"), NamedTextColor.RED));
                        return;
                    }

                    try {
                        replaceRuntime(config, true);
                        sender.sendMessage(Component.text(t("plugin.reload.message.complete"), NamedTextColor.GREEN));
                    } catch (RuntimeRollbackException rollback) {
                        sender.sendMessage(Component.text(t("plugin.reload.message.rolled_back"), NamedTextColor.GOLD));
                    } catch (RuntimeException exception) {
                        getLogger().log(Level.SEVERE, t("plugin.reload.failure.apply"), exception);
                        sender.sendMessage(Component.text(t("plugin.reload.message.failed_apply"), NamedTextColor.RED));
                        getServer().getPluginManager().disablePlugin(this);
                    }
                }));
    }

    public void regenerateWebToken(CommandSender sender) {
        CompletableFuture.supplyAsync(() -> {
                    BootstrapConfigLoader loader = new BootstrapConfigLoader(this);
                    String token = loader.regenerateWebAuthToken();
                    ServerScopeConfig config = loader.loadFromDisk();
                    return new TokenReloadResult(token, config);
                }, configExecutor)
                .whenComplete((result, throwable) -> runPlatformTask(() -> {
                    if (throwable != null) {
                        getLogger().log(Level.SEVERE, t("plugin.web_token.failure"), throwable);
                        sender.sendMessage(Component.text(t("plugin.web_token.message.failed"), NamedTextColor.RED));
                        return;
                    }

                    try {
                        replaceRuntime(result.config(), true);
                        sender.sendMessage(Component.text(t("plugin.web_token.message.complete"), NamedTextColor.GREEN));
                        sender.sendMessage(Component.text(t("plugin.web_token.message.value", java.util.Map.of("token", result.token())), NamedTextColor.YELLOW));
                    } catch (RuntimeRollbackException rollback) {
                        sender.sendMessage(Component.text(t("plugin.reload.message.rolled_back"), NamedTextColor.GOLD));
                    } catch (RuntimeException exception) {
                        getLogger().log(Level.SEVERE, t("plugin.reload.failure.apply"), exception);
                        sender.sendMessage(Component.text(t("plugin.reload.message.failed_apply"), NamedTextColor.RED));
                        getServer().getPluginManager().disablePlugin(this);
                    }
                }));
    }

    private void replaceRuntime(ServerScopeConfig config, boolean reload) {
        synchronized (runtimeLock) {
            PluginRuntime oldRuntime = runtime;
            ServerScopeConfig previousConfig = activeConfig;

            // The embedded web server binds a fixed port, so the old runtime must be released
            // before the new one can start. We keep the previous config so a failed reload can
            // be rolled back instead of leaving the server with no monitoring at all.
            stopRuntimeQuietly(oldRuntime);

            PluginRuntime newRuntime = tryBuildAndStart(config);
            if (newRuntime != null) {
                installRuntime(newRuntime, config);
                if (reload && config.debug().logConfigReloads()) {
                    getLogger().info(t("plugin.reload.runtime_reloaded"));
                }
                return;
            }

            // New runtime failed to start. For a reload, try to restore the last good runtime
            // so a bad config edit does not take the whole plugin offline.
            if (reload && previousConfig != null) {
                PluginRuntime recovered = tryBuildAndStart(previousConfig);
                if (recovered != null) {
                    installRuntime(recovered, previousConfig);
                    throw new RuntimeRollbackException();
                }
            }

            throw new IllegalStateException("Failed to start ServerScope runtime");
        }
    }

    private PluginRuntime tryBuildAndStart(ServerScopeConfig config) {
        PluginRuntime candidate;
        try {
            candidate = new PluginWiringFactory(this, getLogger()).create(config);
            candidate.lifecycleManager().startAll();
            return candidate;
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, t("plugin.reload.failure.apply"), exception);
            return null;
        }
    }

    private void installRuntime(PluginRuntime newRuntime, ServerScopeConfig config) {
        if (newRuntime.runtimeInfoService() instanceof DefaultRuntimeInfoService runtimeInfoService) {
            runtimeInfoService.markStarted();
        }
        runtime = newRuntime;
        translations = newRuntime.translations();
        activeConfig = config;
    }

    private void stopRuntimeQuietly(PluginRuntime target) {
        if (target == null) {
            return;
        }
        try {
            if (target.runtimeInfoService() instanceof DefaultRuntimeInfoService runtimeInfoService) {
                runtimeInfoService.markStopped();
            }
            target.lifecycleManager().stopAll();
        } catch (RuntimeException stopException) {
            getLogger().log(Level.SEVERE, t("plugin.runtime.cleanup_failed"), stopException);
        }
    }

    private void runPlatformTask(Runnable runnable) {
        try {
            var getter = getServer().getClass().getMethod("getGlobalRegionScheduler");
            Object scheduler = getter.invoke(getServer());
            var run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
            run.invoke(scheduler, this, (java.util.function.Consumer<Object>) ignored -> runnable.run());
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        Bukkit.getScheduler().runTask(this, runnable);
    }

    private String t(String key) {
        TranslationService current = translations;
        return current == null ? key : current.text(key);
    }

    private String t(String key, java.util.Map<String, ?> arguments) {
        TranslationService current = translations;
        return current == null ? key : current.text(key, arguments);
    }

    private record TokenReloadResult(String token, ServerScopeConfig config) {
    }

    /** Signals that applying a new config failed but the previous runtime was restored. */
    private static final class RuntimeRollbackException extends RuntimeException {
        private RuntimeRollbackException() {
            super("Reload failed; rolled back to the previous configuration");
        }
    }
}
