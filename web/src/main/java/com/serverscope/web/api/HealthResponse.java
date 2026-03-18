package com.serverscope.web.api;

import java.time.Instant;

public record HealthResponse(
        String status,
        Instant timestamp
) {
}
