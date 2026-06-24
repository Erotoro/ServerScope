package com.serverscope.storage;

import com.serverscope.api.config.StorageConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.api.storage.AlertRecord;
import com.serverscope.api.storage.AnalyzerFindingRecord;
import com.serverscope.api.storage.ChunkSnapshot;
import com.serverscope.api.storage.EventProfileSnapshot;
import com.serverscope.api.storage.MetricSample;
import com.serverscope.api.storage.PluginProfileSnapshot;
import com.serverscope.api.storage.StorageService;
import com.serverscope.api.storage.WorldSnapshot;
import com.serverscope.core.lifecycle.AbstractManagedComponent;
import com.serverscope.storage.runtime.StorageExportService;
import com.serverscope.storage.sqlite.SqliteStorageService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class StorageModule extends AbstractManagedComponent implements StorageService {
    private final Logger logger;
    private final SqliteStorageService delegate;
    private final StorageExportService exportService;

    public StorageModule(Logger logger, StorageConfig config) {
        this(
                logger,
                config,
                Map::of,
                () -> new ProfilerSnapshot(java.time.Instant.EPOCH, List.of(), List.of(), List.of(), List.of()),
                List::of,
                List::of
        );
    }

    public StorageModule(
            Logger logger,
            StorageConfig config,
            Supplier<Map<String, MetricBatch>> metricBatchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<com.serverscope.api.alert.AlertRecord>> activeAlertsSupplier,
            Supplier<List<DiagnosticFinding>> activeFindingsSupplier
    ) {
        super("storage");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.delegate = new SqliteStorageService(logger, Objects.requireNonNull(config, "config"));
        this.exportService = new StorageExportService(
                logger,
                config,
                delegate,
                Objects.requireNonNull(metricBatchesSupplier, "metricBatchesSupplier"),
                Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier"),
                Objects.requireNonNull(activeAlertsSupplier, "activeAlertsSupplier"),
                Objects.requireNonNull(activeFindingsSupplier, "activeFindingsSupplier")
        );
    }

    @Override
    public void start() {
        delegate.start();
        try {
            exportService.start();
            updateHealth(ComponentStatus.RUNNING, "Storage service worker started");
            logger.info("Storage module initialized");
        } catch (RuntimeException exception) {
            delegate.stop();
            throw exception;
        }
    }

    @Override
    public void stop() {
        RuntimeException failure = null;
        try {
            exportService.stop();
        } catch (RuntimeException exception) {
            failure = exception;
        } finally {
            try {
                delegate.stop();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        updateHealth(ComponentStatus.STOPPED, "Storage service stopped");
        logger.info("Storage module stopped");
        if (failure != null) {
            throw failure;
        }
    }

    @Override
    public boolean enqueueMetricSample(MetricSample sample) {
        return delegate.enqueueMetricSample(sample);
    }

    @Override
    public boolean enqueueAlert(AlertRecord alertRecord) {
        return delegate.enqueueAlert(alertRecord);
    }

    @Override
    public boolean enqueuePluginProfileSnapshot(PluginProfileSnapshot snapshot) {
        return delegate.enqueuePluginProfileSnapshot(snapshot);
    }

    @Override
    public boolean enqueueEventProfileSnapshot(EventProfileSnapshot snapshot) {
        return delegate.enqueueEventProfileSnapshot(snapshot);
    }

    @Override
    public boolean enqueueWorldSnapshot(WorldSnapshot snapshot) {
        return delegate.enqueueWorldSnapshot(snapshot);
    }

    @Override
    public boolean enqueueChunkSnapshot(ChunkSnapshot snapshot) {
        return delegate.enqueueChunkSnapshot(snapshot);
    }

    @Override
    public boolean enqueueAnalyzerFinding(AnalyzerFindingRecord record) {
        return delegate.enqueueAnalyzerFinding(record);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public void requestRetentionCleanup() {
        delegate.requestRetentionCleanup();
    }

    @Override
    public List<MetricSample> findLatestMetricSamples(int limit) {
        return delegate.findLatestMetricSamples(limit);
    }

    @Override
    public List<MetricSample> findMetricSamplesSince(java.time.Instant since, int limit) {
        return delegate.findMetricSamplesSince(since, limit);
    }

    @Override
    public List<AlertRecord> findLatestAlerts(int limit) {
        return delegate.findLatestAlerts(limit);
    }

    @Override
    public List<PluginProfileSnapshot> findLatestPluginProfileSnapshots(int limit) {
        return delegate.findLatestPluginProfileSnapshots(limit);
    }

    @Override
    public List<EventProfileSnapshot> findLatestEventProfileSnapshots(int limit) {
        return delegate.findLatestEventProfileSnapshots(limit);
    }

    @Override
    public List<WorldSnapshot> findLatestWorldSnapshots(int limit) {
        return delegate.findLatestWorldSnapshots(limit);
    }

    @Override
    public List<ChunkSnapshot> findLatestChunkSnapshots(int limit) {
        return delegate.findLatestChunkSnapshots(limit);
    }

    @Override
    public List<AnalyzerFindingRecord> findLatestAnalyzerFindings(int limit) {
        return delegate.findLatestAnalyzerFindings(limit);
    }
}
