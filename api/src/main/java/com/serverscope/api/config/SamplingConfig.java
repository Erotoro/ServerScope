package com.serverscope.api.config;

public record SamplingConfig(
        boolean skipOverlappingChunkSampling,
        int defaultMaxChunksPerRun,
        long defaultIntervalMillis
) {
    public SamplingConfig {
        if (defaultMaxChunksPerRun <= 0) {
            throw new IllegalArgumentException("defaultMaxChunksPerRun must be positive");
        }
        if (defaultIntervalMillis <= 0L) {
            throw new IllegalArgumentException("defaultIntervalMillis must be positive");
        }
    }
}
