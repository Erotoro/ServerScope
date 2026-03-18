package com.serverscope.api.storage;

import java.util.List;

public interface WorldSnapshotRepository {
    List<WorldSnapshot> findLatestWorldSnapshots(int limit);
}
