package com.serverscope.api.storage;

import java.time.Instant;
import java.util.Objects;

public record WorldSnapshot(
        Instant sampleTime,
        String worldName,
        long loadedChunks
) {
    public WorldSnapshot {
        Objects.requireNonNull(sampleTime, "sampleTime");
        Objects.requireNonNull(worldName, "worldName");
    }
}
