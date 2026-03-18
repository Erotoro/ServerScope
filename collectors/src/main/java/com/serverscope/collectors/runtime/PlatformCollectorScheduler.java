package com.serverscope.collectors.runtime;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

public final class PlatformCollectorScheduler {
    private final JavaPlugin plugin;

    public PlatformCollectorScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public CollectorTaskHandle scheduleRepeating(Runnable runnable, long initialDelayMillis, long intervalMillis) {
        long initialDelayTicks = millisToTicks(initialDelayMillis);
        long intervalTicks = millisToTicks(intervalMillis);

        Object globalRegionScheduler = resolveGlobalRegionScheduler();
        if (globalRegionScheduler != null) {
            Object taskHandle = scheduleWithGlobalRegionScheduler(globalRegionScheduler, runnable, initialDelayTicks, intervalTicks);
            if (taskHandle != null) {
                return () -> cancelReflectively(taskHandle);
            }
        }

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, initialDelayTicks, intervalTicks);
        return bukkitTask::cancel;
    }

    public void execute(Runnable runnable) {
        Object globalRegionScheduler = resolveGlobalRegionScheduler();
        if (globalRegionScheduler != null) {
            if (runWithGlobalRegionScheduler(globalRegionScheduler, runnable)) {
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private Object resolveGlobalRegionScheduler() {
        try {
            Method method = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler");
            return method.invoke(plugin.getServer());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object scheduleWithGlobalRegionScheduler(
            Object scheduler,
            Runnable runnable,
            long initialDelayTicks,
            long intervalTicks
    ) {
        try {
            Method method = scheduler.getClass().getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class
            );
            return method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run(), initialDelayTicks, intervalTicks);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private boolean runWithGlobalRegionScheduler(Object scheduler, Runnable runnable) {
        try {
            Method method = scheduler.getClass().getMethod(
                    "run",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class
            );
            method.invoke(scheduler, plugin, (Consumer<Object>) ignored -> runnable.run());
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private void cancelReflectively(Object taskHandle) {
        try {
            Method cancel = taskHandle.getClass().getMethod("cancel");
            cancel.invoke(taskHandle);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private long millisToTicks(long millis) {
        return Math.max(1L, (long) Math.ceil(millis / 50.0d));
    }
}
