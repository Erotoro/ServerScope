package com.serverscope.collectors.world;

import java.util.Objects;

public record ChunkCoordinate(String worldName, int chunkX, int chunkZ) {
    public ChunkCoordinate {
        Objects.requireNonNull(worldName, "worldName");
    }
}
