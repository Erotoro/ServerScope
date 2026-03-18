package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record AnalyzerFindingRecord(
        Instant eventTime,
        String findingCode,
        AnalyzerFindingSeverity severity,
        String subject,
        String message,
        String detailsJson
) {
    public AnalyzerFindingRecord {
        Objects.requireNonNull(eventTime, "eventTime");
        Objects.requireNonNull(findingCode, "findingCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(detailsJson, "detailsJson");
    }
}
