package com.serverscope.analyzer.diagnostic;

import java.time.Instant;

public record ServerHealthSample(
        Instant timestamp,
        double tps,
        double mspt,
        long heapUsedBytes,
        long heapMaxBytes,
        long loadedChunks,
        long totalEntities,
        int playersOnline
) {
}
