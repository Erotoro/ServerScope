package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record PluginProfileSnapshot(
        Instant sampleTime,
        String pluginName,
        String eventName,
        String listenerClass,
        long callsCount,
        long totalTimeNanos,
        long maxTimeNanos,
        long p95TimeNanos
) {
    public PluginProfileSnapshot {
        Objects.requireNonNull(sampleTime, "sampleTime");
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(eventName, "eventName");
        Objects.requireNonNull(listenerClass, "listenerClass");
    }
}
