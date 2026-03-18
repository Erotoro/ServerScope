package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record MetricSample(
        Instant sampleTime,
        double tps,
        double mspt,
        long heapUsedBytes,
        int onlinePlayers,
        int worldCount,
        long loadedChunks,
        long totalEntities
) {
    public MetricSample {
        Objects.requireNonNull(sampleTime, "sampleTime");
    }
}
