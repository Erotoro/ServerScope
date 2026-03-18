package com.serverscope.collectors.profile;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class EventProfilingRegistrar {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final EventProfilingService profilingService;
    private final List<Listener> listeners = new ArrayList<>();

    public EventProfilingRegistrar(JavaPlugin plugin, Logger logger, EventProfilingService profilingService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.profilingService = Objects.requireNonNull(profilingService, "profilingService");
    }

    public void register(ProfiledEventBinding binding) {
        Listener listener = new Listener() {
        };
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvent(binding.eventClass(), listener, EventPriority.LOWEST, startExecutor(binding), plugin, false);
        pluginManager.registerEvent(binding.eventClass(), listener, EventPriority.MONITOR, endExecutor(binding), plugin, false);
        listeners.add(listener);
        logger.info("Registered event profiler for " + binding.id());
    }

    public void unregisterAll() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }

    private EventExecutor startExecutor(ProfiledEventBinding binding) {
        return (ignored, event) -> {
            if (binding.eventClass().isInstance(event)) {
                profilingService.onEventStart(binding, cast(event));
            }
        };
    }

    private EventExecutor endExecutor(ProfiledEventBinding binding) {
        return (ignored, event) -> {
            if (binding.eventClass().isInstance(event)) {
                profilingService.onEventEnd(binding, cast(event));
            }
        };
    }

    private Event cast(Event event) {
        return event;
    }
}
