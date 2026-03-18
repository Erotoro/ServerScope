package com.serverscope.api.storage;

import java.util.List;

public interface ChunkSnapshotRepository {
    List<ChunkSnapshot> findLatestChunkSnapshots(int limit);
}
