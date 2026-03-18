package com.serverscope.api.config;

import java.util.Objects;

public record AlertingConfig(
        boolean enabled,
        long evaluationIntervalMillis,
        long cooldownMillis,
        long rateLimitMillis,
        AlertThresholdsConfig thresholds,
        AlertChannelsConfig channels
) {
    public AlertingConfig {
        Objects.requireNonNull(thresholds, "thresholds");
        Objects.requireNonNull(channels, "channels");
        if (evaluationIntervalMillis <= 0L) {
            throw new IllegalArgumentException("evaluationIntervalMillis must be positive");
        }
        if (cooldownMillis < 0L) {
            throw new IllegalArgumentException("cooldownMillis must not be negative");
        }
        if (rateLimitMillis < 0L) {
            throw new IllegalArgumentException("rateLimitMillis must not be negative");
        }
    }
}
