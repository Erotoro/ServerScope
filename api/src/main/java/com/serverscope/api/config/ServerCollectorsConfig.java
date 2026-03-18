package com.serverscope.api.config;

public record ServerCollectorsConfig(
        long tpsMsptIntervalMillis,
        long playersIntervalMillis,
        long jvmIntervalMillis,
        long loadedChunksIntervalMillis,
        long totalEntitiesIntervalMillis
) {
    public ServerCollectorsConfig {
        validatePositive(tpsMsptIntervalMillis, "tpsMsptIntervalMillis");
        validatePositive(playersIntervalMillis, "playersIntervalMillis");
        validatePositive(jvmIntervalMillis, "jvmIntervalMillis");
        validatePositive(loadedChunksIntervalMillis, "loadedChunksIntervalMillis");
        validatePositive(totalEntitiesIntervalMillis, "totalEntitiesIntervalMillis");
    }

    private static void validatePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
