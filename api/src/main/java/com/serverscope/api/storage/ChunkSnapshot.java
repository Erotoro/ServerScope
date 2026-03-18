package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record ChunkSnapshot(
        Instant sampleTime,
        String worldName,
        int chunkX,
        int chunkZ,
        int entityCount,
        int blockEntityCount,
        long hotspotScore
) {
    public ChunkSnapshot {
        Objects.requireNonNull(sampleTime, "sampleTime");
        Objects.requireNonNull(worldName, "worldName");
    }
}
