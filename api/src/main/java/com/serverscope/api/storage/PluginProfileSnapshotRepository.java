package com.serverscope.api.storage;

import java.util.List;

public interface PluginProfileSnapshotRepository {
    List<PluginProfileSnapshot> findLatestPluginProfileSnapshots(int limit);
}
