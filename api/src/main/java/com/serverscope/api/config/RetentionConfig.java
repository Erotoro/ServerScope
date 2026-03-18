package com.serverscope.api.config;

public record RetentionConfig(
        boolean enabled,
        int metricSamplesDays,
        int alertsDays,
        int profilingDays,
        int chunkSnapshotsDays
) {
    public RetentionConfig {
        validatePositive(metricSamplesDays, "metricSamplesDays");
        validatePositive(alertsDays, "alertsDays");
        validatePositive(profilingDays, "profilingDays");
        validatePositive(chunkSnapshotsDays, "chunkSnapshotsDays");
    }

    private static void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
