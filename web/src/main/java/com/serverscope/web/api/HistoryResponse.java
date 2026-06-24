package com.serverscope.web.api;

import java.time.Instant;
import java.util.List;

/**
 * Time-ranged history of aggregate server-health samples backed by persistent storage.
 *
 * @param capturedAt    when this response was produced
 * @param fromTime      inclusive lower bound of the requested window
 * @param windowMinutes width of the requested window in minutes
 * @param points        samples ordered from oldest to newest
 */
public record HistoryResponse(
        Instant capturedAt,
        Instant fromTime,
        int windowMinutes,
        List<HistoryPointResponse> points
) {
}
