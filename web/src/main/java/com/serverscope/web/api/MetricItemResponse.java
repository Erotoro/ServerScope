package com.serverscope.web.api;

import java.time.Instant;
import java.util.Map;

public record MetricItemResponse(
        String collectorId,
        String metricType,
        double value,
        Instant timestamp,
        Map<String, String> labels
) {
}
