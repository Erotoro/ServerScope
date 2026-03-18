package com.serverscope.api.alert;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AlertRecord(
        String code,
        String dedupeKey,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        Instant occurredAt,
        Map<String, String> labels
) {
    public AlertRecord {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(dedupeKey, "dedupeKey");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(labels, "labels");
        labels = Map.copyOf(labels);
    }
}
