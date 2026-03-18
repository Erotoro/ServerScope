package com.serverscope.analyzer.diagnostic;

import java.time.Instant;

public record ChunkDiagnosticSample(
        Instant timestamp,
        String worldName,
        int chunkX,
        int chunkZ,
        long entityCount,
        long blockEntityCount,
        long hotspotScore
) {
}
