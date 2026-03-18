package com.serverscope.web.api;

import java.time.Instant;

public record WorldItemResponse(
        String world,
        long loadedChunks,
        Instant timestamp
) {
}
