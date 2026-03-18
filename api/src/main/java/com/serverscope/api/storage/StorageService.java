package com.serverscope.api.storage;

public interface StorageService extends MetricSampleRepository,
        AlertRepository,
        PluginProfileSnapshotRepository,
        EventProfileSnapshotRepository,
        WorldSnapshotRepository,
        ChunkSnapshotRepository,
        AnalyzerFindingRepository {
    boolean enqueueMetricSample(MetricSample sample);

    boolean enqueueAlert(AlertRecord alertRecord);

    boolean enqueuePluginProfileSnapshot(PluginProfileSnapshot snapshot);

    boolean enqueueEventProfileSnapshot(EventProfileSnapshot snapshot);

    boolean enqueueWorldSnapshot(WorldSnapshot snapshot);

    boolean enqueueChunkSnapshot(ChunkSnapshot snapshot);

    boolean enqueueAnalyzerFinding(AnalyzerFindingRecord record);

    void flush();

    void requestRetentionCleanup();
}
