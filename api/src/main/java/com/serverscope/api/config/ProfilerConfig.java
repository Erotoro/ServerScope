package com.serverscope.api.config;

import java.util.List;
import java.util.Objects;

public record ProfilerConfig(
        boolean enabled,
        List<String> eventIds,
        int topLimit,
        long burstWindowMillis,
        long burstMinimumCount
) {
    public ProfilerConfig {
        Objects.requireNonNull(eventIds, "eventIds");
        eventIds = List.copyOf(eventIds);
        if (topLimit <= 0) {
            throw new IllegalArgumentException("topLimit must be positive");
        }
        if (burstWindowMillis <= 0L) {
            throw new IllegalArgumentException("burstWindowMillis must be positive");
        }
        if (burstMinimumCount <= 0L) {
            throw new IllegalArgumentException("burstMinimumCount must be positive");
        }
    }
}
