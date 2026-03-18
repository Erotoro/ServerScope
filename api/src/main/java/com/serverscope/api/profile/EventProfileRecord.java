package com.serverscope.api.profile;

import java.util.Objects;
import java.util.Set;

public record EventProfileRecord(
        String eventId,
        String eventClassName,
        long count,
        long totalTimeNanos,
        long maxTimeNanos,
        long averageTimeNanos,
        long maxWindowCount,
        double burstScore,
        long burstWindowMillis,
        Set<String> participatingPlugins
) {
    public EventProfileRecord {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventClassName, "eventClassName");
        Objects.requireNonNull(participatingPlugins, "participatingPlugins");
        participatingPlugins = Set.copyOf(participatingPlugins);
    }
}
