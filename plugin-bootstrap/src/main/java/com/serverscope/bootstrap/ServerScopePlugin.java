package com.serverscope.bootstrap;

import com.serverscope.bootstrap.command.ServerScopeCommandExecutor;
import com.serverscope.bootstrap.config.BootstrapConfigLoader;
import com.serverscope.bootstrap.wiring.PluginRuntime;
import com.serverscope.bootstrap.wiring.PluginWiringFactory;
import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.ServerScopeConfig;
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
    private final ExecutorService configExecutor = Executors.newSingleThreadExecutor(runnable ->
            Thread.ofPlatform().name("serverscope-config-loader").daemon(true).unstarted(runnable));
    private volatile PluginRuntime runtime;
    private volatile TranslationService translations;

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
            PluginRuntime newRuntime = new PluginWiringFactory(this, getLogger()).create(config);

            if (oldRuntime != null) {
                if (oldRuntime.runtimeInfoService() instanceof DefaultRuntimeInfoService runtimeInfoService) {
                    runtimeInfoService.markStopped();
                }
                oldRuntime.lifecycleManager().stopAll();
            }

            try {
                newRuntime.lifecycleManager().startAll();
                if (newRuntime.runtimeInfoService() instanceof DefaultRuntimeInfoService runtimeInfoService) {
                    runtimeInfoService.markStarted();
                }
                runtime = newRuntime;
                translations = newRuntime.translations();
            } catch (RuntimeException exception) {
                try {
                    newRuntime.lifecycleManager().stopAll();
                } catch (RuntimeException stopException) {
                    getLogger().log(Level.SEVERE, t("plugin.runtime.cleanup_failed"), stopException);
                }
                throw exception;
            }

            if (reload && config.debug().logConfigReloads()) {
                getLogger().info(t("plugin.reload.runtime_reloaded"));
            }
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
}
