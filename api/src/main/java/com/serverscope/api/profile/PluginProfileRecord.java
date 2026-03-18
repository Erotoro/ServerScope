package com.serverscope.api.profile;

import java.util.Objects;
import java.util.Set;

public record PluginProfileRecord(
        String pluginName,
        long eventCount,
        long attributedTotalTimeNanos,
        long maxAttributedTimeNanos,
        long averageAttributedTimeNanos,
        Set<String> observedEvents
) {
    public PluginProfileRecord {
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(observedEvents, "observedEvents");
        observedEvents = Set.copyOf(observedEvents);
    }
}
