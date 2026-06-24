package com.serverscope.web.api;

import java.time.Instant;

/**
 * A single aggregate server-health sample, shaped for direct rendering in time-series charts.
 */
public record HistoryPointResponse(
        Instant time,
        double tps,
        double mspt,
        long heapUsedBytes,
        int onlinePlayers,
        int worldCount,
        long loadedChunks,
        long totalEntities
) {
}
