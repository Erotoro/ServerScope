package com.serverscope.api.config;

import java.nio.file.Path;
import java.util.Objects;

public record StorageConfig(
        Path sqliteFile,
        boolean enabled,
        int queueCapacity,
        int maxBatchSize,
        long flushIntervalMillis,
        int retentionDays
) {
    public StorageConfig {
        Objects.requireNonNull(sqliteFile, "sqliteFile");
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be positive");
        }
        if (flushIntervalMillis <= 0) {
            throw new IllegalArgumentException("flushIntervalMillis must be positive");
        }
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
    }
}
