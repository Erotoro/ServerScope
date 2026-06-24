package com.serverscope.web.api;

import java.time.Instant;

/**
 * A persisted alert record read back from storage, shaped for the dashboard alert history view.
 */
public record AlertHistoryItemResponse(
        Instant occurredAt,
        String code,
        String severity,
        String status,
        String dedupeKey,
        String message
) {
}
