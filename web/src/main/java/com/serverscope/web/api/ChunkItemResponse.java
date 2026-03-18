package com.serverscope.web.api;

import java.time.Instant;

public record ChunkItemResponse(
        String world,
        int chunkX,
        int chunkZ,
        long entityCount,
        long blockEntityCount,
        Instant timestamp
) {
}
