package com.serverscope.api.diagnostic;

import java.time.Instant;
import java.util.Objects;

public record DiagnosticFinding(
        String id,
        Instant timestamp,
        FindingSeverity severity,
        String title,
        String description,
        String probableCause,
        String suggestedAction,
        FindingConfidence confidence,
        DiagnosticReference reference
) {
    public DiagnosticFinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(probableCause, "probableCause");
        Objects.requireNonNull(suggestedAction, "suggestedAction");
        Objects.requireNonNull(confidence, "confidence");
    }
}
