package com.serverscope.api.config;

public record AlertThresholdsConfig(
        double lowTps,
        double highMspt,
        long highEntityCount,
        long highChunkEntityCount,
        long highChunkBlockEntityCount,
        double highEventAverageMillis
) {
    public AlertThresholdsConfig {
        if (lowTps <= 0.0d) {
            throw new IllegalArgumentException("lowTps must be positive");
        }
        if (highMspt <= 0.0d) {
            throw new IllegalArgumentException("highMspt must be positive");
        }
        if (highEntityCount <= 0L) {
            throw new IllegalArgumentException("highEntityCount must be positive");
        }
        if (highChunkEntityCount <= 0L) {
            throw new IllegalArgumentException("highChunkEntityCount must be positive");
        }
        if (highChunkBlockEntityCount <= 0L) {
            throw new IllegalArgumentException("highChunkBlockEntityCount must be positive");
        }
        if (highEventAverageMillis <= 0.0d) {
            throw new IllegalArgumentException("highEventAverageMillis must be positive");
        }
    }
}
