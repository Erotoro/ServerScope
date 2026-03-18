package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record AlertRecord(
        Instant eventTime,
        String alertCode,
        AlertSeverity severity,
        AlertStatus status,
        String dedupeKey,
        String message,
        String dimensionsJson
) {
    public AlertRecord {
        Objects.requireNonNull(eventTime, "eventTime");
        Objects.requireNonNull(alertCode, "alertCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(dedupeKey, "dedupeKey");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(dimensionsJson, "dimensionsJson");
    }
}
