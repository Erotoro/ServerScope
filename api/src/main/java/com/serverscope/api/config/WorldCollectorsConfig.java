package com.serverscope.api.config;

public record WorldCollectorsConfig(
        long worldSnapshotIntervalMillis,
        long chunkSamplingIntervalMillis,
        int maxChunksPerRun
) {
    public WorldCollectorsConfig {
        if (worldSnapshotIntervalMillis <= 0) {
            throw new IllegalArgumentException("worldSnapshotIntervalMillis must be positive");
        }
        if (chunkSamplingIntervalMillis <= 0) {
            throw new IllegalArgumentException("chunkSamplingIntervalMillis must be positive");
        }
        if (maxChunksPerRun <= 0) {
            throw new IllegalArgumentException("maxChunksPerRun must be positive");
        }
    }
}
