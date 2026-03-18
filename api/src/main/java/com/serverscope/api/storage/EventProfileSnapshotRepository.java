package com.serverscope.api.storage;

import java.util.List;

public interface EventProfileSnapshotRepository {
    List<EventProfileSnapshot> findLatestEventProfileSnapshots(int limit);
}
