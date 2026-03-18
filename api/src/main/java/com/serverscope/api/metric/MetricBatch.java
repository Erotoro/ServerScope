package com.serverscope.api.metric;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MetricBatch(
        String collectorId,
        Instant collectedAt,
        List<MetricSample> samples
) {
    public MetricBatch {
        Objects.requireNonNull(collectorId, "collectorId");
        Objects.requireNonNull(collectedAt, "collectedAt");
        Objects.requireNonNull(samples, "samples");
        samples = List.copyOf(samples);
    }

    public boolean isEmpty() {
        return samples.isEmpty();
    }
}
