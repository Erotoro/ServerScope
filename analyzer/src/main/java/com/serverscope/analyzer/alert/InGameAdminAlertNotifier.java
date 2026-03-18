package com.serverscope.analyzer.alert;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.alert.AlertSeverity;
import com.serverscope.api.config.AlertChannelsConfig;
import com.serverscope.core.i18n.TranslationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

public final class InGameAdminAlertNotifier implements AlertNotifier {
    private final JavaPlugin plugin;
    private final AlertChannelsConfig config;
    private final TranslationService translations;

    public InGameAdminAlertNotifier(JavaPlugin plugin, AlertChannelsConfig config, TranslationService translations) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
    }

    @Override
    public void notify(AlertRecord alertRecord) {
        if (!config.inGameEnabled() || alertRecord.severity() != AlertSeverity.CRITICAL) {
            return;
        }
        schedulePlatformSafe(() -> {
            NamedTextColor color = NamedTextColor.DARK_RED;
            Component message = Component.text(
                    translations.text("alert.channel.ingame.format", java.util.Map.of(
                            "severity", translations.text("enum.severity." + alertRecord.severity().name().toLowerCase(java.util.Locale.ROOT)),
                            "status", translations.text("enum.status." + alertRecord.status().name().toLowerCase(java.util.Locale.ROOT)),
                            "message", alertRecord.message()
                    )),
                    color
            );
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(config.adminPermission())) {
                    player.sendMessage(message);
                }
            }
        });
    }

    private void schedulePlatformSafe(Runnable runnable) {
        try {
            Method getter = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
            Object scheduler = getter.invoke(plugin.getServer());
            Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class);
            run.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run());
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
