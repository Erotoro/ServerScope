package com.serverscope.web.api;

import java.time.Instant;

public record MetricValueResponse(
        double value,
        Instant timestamp
) {
}
