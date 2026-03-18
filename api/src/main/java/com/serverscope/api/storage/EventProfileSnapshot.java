package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record EventProfileSnapshot(
        Instant sampleTime,
        String snapshotKind,
        String eventId,
        String eventClassName,
        long count,
        long totalTimeNanos,
        long maxTimeNanos,
        long averageTimeNanos,
        long maxWindowCount,
        double burstScore
) {
    public EventProfileSnapshot {
        Objects.requireNonNull(sampleTime, "sampleTime");
        Objects.requireNonNull(snapshotKind, "snapshotKind");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventClassName, "eventClassName");
    }
}
