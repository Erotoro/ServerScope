package com.serverscope.storage.sqlite;

import com.serverscope.api.config.StorageConfig;
import com.serverscope.api.storage.AlertRecord;
import com.serverscope.api.storage.AnalyzerFindingRecord;
import com.serverscope.api.storage.ChunkSnapshot;
import com.serverscope.api.storage.EventProfileSnapshot;
import com.serverscope.api.storage.MetricSample;
import com.serverscope.api.storage.PluginProfileSnapshot;
import com.serverscope.api.storage.StorageService;
import com.serverscope.api.storage.WorldSnapshot;
import com.serverscope.storage.sqlite.migration.SqliteMigrationRunner;
import com.serverscope.storage.sqlite.repository.SqliteAlertRepository;
import com.serverscope.storage.sqlite.repository.SqliteAnalyzerFindingRepository;
import com.serverscope.storage.sqlite.repository.SqliteChunkSnapshotRepository;
import com.serverscope.storage.sqlite.repository.SqliteEventProfileSnapshotRepository;
import com.serverscope.storage.sqlite.repository.SqliteMetricSampleRepository;
import com.serverscope.storage.sqlite.repository.SqlitePluginProfileSnapshotRepository;
import com.serverscope.storage.sqlite.repository.SqliteWorldSnapshotRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SqliteStorageService implements StorageService {
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 10L;
    private static final long RETENTION_CHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(6);
    private static final long QUEUE_OFFER_TIMEOUT_MILLIS = 250L;
    private static final long DROP_LOG_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final Logger logger;
    private final StorageConfig config;
    private final BlockingQueue<Object> queue;
    private final AtomicBoolean acceptingWrites;
    private final SqliteConnectionFactory connectionFactory;
    private final SqliteMigrationRunner migrationRunner;
    private final SqliteMetricSampleRepository metricRepository;
    private final SqliteAlertRepository alertRepository;
    private final SqlitePluginProfileSnapshotRepository pluginProfileRepository;
    private final SqliteEventProfileSnapshotRepository eventProfileSnapshotRepository;
    private final SqliteWorldSnapshotRepository worldSnapshotRepository;
    private final SqliteChunkSnapshotRepository chunkSnapshotRepository;
    private final SqliteAnalyzerFindingRepository analyzerFindingRepository;
    private final CountDownLatch initialized;
    private final AtomicReference<Throwable> initializationFailure;
    private final AtomicLong droppedWriteCount;

    private volatile Thread workerThread;
    private volatile boolean running;
    private volatile long lastRetentionCleanupAt;
    private volatile long lastDropLogAt;
    private volatile boolean stopRequested;
    private volatile boolean flushRequested;
    private volatile boolean retentionCleanupRequested;

    public SqliteStorageService(Logger logger, StorageConfig config) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.queue = new ArrayBlockingQueue<>(config.queueCapacity());
        this.acceptingWrites = new AtomicBoolean(false);
        this.connectionFactory = new SqliteConnectionFactory(config);
        this.migrationRunner = new SqliteMigrationRunner(connectionFactory);
        this.metricRepository = new SqliteMetricSampleRepository();
        this.alertRepository = new SqliteAlertRepository();
        this.pluginProfileRepository = new SqlitePluginProfileSnapshotRepository();
        this.eventProfileSnapshotRepository = new SqliteEventProfileSnapshotRepository();
        this.worldSnapshotRepository = new SqliteWorldSnapshotRepository();
        this.chunkSnapshotRepository = new SqliteChunkSnapshotRepository();
        this.analyzerFindingRepository = new SqliteAnalyzerFindingRepository();
        this.initialized = new CountDownLatch(1);
        this.initializationFailure = new AtomicReference<>();
        this.droppedWriteCount = new AtomicLong();
    }

    public void start() {
        if (!config.enabled()) {
            logger.info("Storage is disabled by configuration");
            initialized.countDown();
            return;
        }
        if (running) {
            return;
        }

        running = true;
        acceptingWrites.set(true);
        stopRequested = false;
        flushRequested = false;
        retentionCleanupRequested = false;
        initializationFailure.set(null);
        workerThread = Thread.ofPlatform().name("serverscope-storage-writer").daemon(false).start(this::runWriterLoop);
        awaitInitializationOrThrow();
    }

    public void stop() {
        if (!config.enabled() || !running) {
            return;
        }

        acceptingWrites.set(false);
        flushRequested = true;
        stopRequested = true;

        try {
            Thread thread = workerThread;
            if (thread != null) {
                thread.interrupt();
                thread.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_TIMEOUT_SECONDS));
                if (thread.isAlive()) {
                    logger.warning("Storage worker did not stop within timeout");
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while waiting for storage worker shutdown", exception);
        } finally {
            running = false;
            workerThread = null;
        }
    }

    @Override
    public boolean enqueueMetricSample(MetricSample sample) {
        return enqueueWrite(sample);
    }

    @Override
    public boolean enqueueAlert(AlertRecord alertRecord) {
        return enqueueWrite(alertRecord);
    }

    @Override
    public boolean enqueuePluginProfileSnapshot(PluginProfileSnapshot snapshot) {
        return enqueueWrite(snapshot);
    }

    @Override
    public boolean enqueueEventProfileSnapshot(EventProfileSnapshot snapshot) {
        return enqueueWrite(snapshot);
    }

    @Override
    public boolean enqueueWorldSnapshot(WorldSnapshot snapshot) {
        return enqueueWrite(snapshot);
    }

    @Override
    public boolean enqueueChunkSnapshot(ChunkSnapshot snapshot) {
        return enqueueWrite(snapshot);
    }

    @Override
    public boolean enqueueAnalyzerFinding(AnalyzerFindingRecord record) {
        return enqueueWrite(record);
    }

    @Override
    public void flush() {
        if (config.enabled()) {
            flushRequested = true;
        }
    }

    @Override
    public void requestRetentionCleanup() {
        if (config.enabled()) {
            retentionCleanupRequested = true;
        }
    }

    @Override
    public List<MetricSample> findLatestMetricSamples(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return metricRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query metric samples", exception);
        }
    }

    @Override
    public List<AlertRecord> findLatestAlerts(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return alertRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query alerts", exception);
        }
    }

    @Override
    public List<PluginProfileSnapshot> findLatestPluginProfileSnapshots(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return pluginProfileRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query plugin profile snapshots", exception);
        }
    }

    @Override
    public List<EventProfileSnapshot> findLatestEventProfileSnapshots(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return eventProfileSnapshotRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query event profile snapshots", exception);
        }
    }

    @Override
    public List<WorldSnapshot> findLatestWorldSnapshots(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return worldSnapshotRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query world snapshots", exception);
        }
    }

    @Override
    public List<ChunkSnapshot> findLatestChunkSnapshots(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return chunkSnapshotRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query chunk snapshots", exception);
        }
    }

    @Override
    public List<AnalyzerFindingRecord> findLatestAnalyzerFindings(int limit) {
        awaitInitialization();
        try (Connection connection = connectionFactory.openConnection()) {
            return analyzerFindingRepository.findLatest(connection, limit);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query analyzer findings", exception);
        }
    }

    private boolean enqueueWrite(Object command) {
        if (!config.enabled() || !acceptingWrites.get()) {
            return false;
        }

        if (queue.remainingCapacity() <= Math.max(1, config.maxBatchSize() / 4)) {
            flushRequested = true;
        }

        try {
            boolean offered = queue.offer(command, QUEUE_OFFER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!offered) {
                flushRequested = true;
                long dropped = droppedWriteCount.incrementAndGet();
                long now = System.currentTimeMillis();
                if (now - lastDropLogAt >= DROP_LOG_INTERVAL_MILLIS) {
                    lastDropLogAt = now;
                    logger.warning("Storage queue remained full after waiting "
                            + QUEUE_OFFER_TIMEOUT_MILLIS
                            + "ms; dropped writes="
                            + dropped);
                }
            }
            return offered;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Interrupted while enqueueing storage command", exception);
            return false;
        }
    }

    private void runWriterLoop() {
        List<MetricSample> metricBatch = new ArrayList<>(config.maxBatchSize());
        List<AlertRecord> alertBatch = new ArrayList<>(config.maxBatchSize());
        List<PluginProfileSnapshot> pluginBatch = new ArrayList<>(config.maxBatchSize());
        List<EventProfileSnapshot> eventProfileBatch = new ArrayList<>(config.maxBatchSize());
        List<WorldSnapshot> worldBatch = new ArrayList<>(config.maxBatchSize());
        List<ChunkSnapshot> chunkBatch = new ArrayList<>(config.maxBatchSize());
        List<AnalyzerFindingRecord> findingBatch = new ArrayList<>(config.maxBatchSize());

        try {
            migrationRunner.migrate();
            lastRetentionCleanupAt = System.currentTimeMillis();
        } catch (SQLException exception) {
            initializationFailure.set(exception);
            acceptingWrites.set(false);
            stopRequested = true;
            logger.log(Level.SEVERE, "Failed to initialize SQLite storage", exception);
            running = false;
        } finally {
            initialized.countDown();
        }

        if (!running) {
            return;
        }

        while (!stopRequested || !queue.isEmpty()) {
            try {
                Object command = queue.poll(config.flushIntervalMillis(), TimeUnit.MILLISECONDS);
                if (command != null) {
                    handleCommand(command, metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
                }

                drainAvailable(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
                flushIfNeeded(
                        command == null,
                        metricBatch,
                        alertBatch,
                        pluginBatch,
                        eventProfileBatch,
                        worldBatch,
                        chunkBatch,
                        findingBatch
                );

                if (retentionCleanupRequested || System.currentTimeMillis() - lastRetentionCleanupAt >= RETENTION_CHECK_INTERVAL_MILLIS) {
                    runRetentionCleanup();
                    lastRetentionCleanupAt = System.currentTimeMillis();
                    retentionCleanupRequested = false;
                }
            } catch (InterruptedException exception) {
                logger.log(Level.FINE, "Storage worker interrupted", exception);
            } catch (SQLException exception) {
                logger.log(Level.SEVERE, "SQLite write cycle failed", exception);
            }
        }

        try {
            flushBatches(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
            runRetentionCleanup();
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Final SQLite flush failed", exception);
        } finally {
            acceptingWrites.set(false);
            running = false;
        }
    }

    private void handleCommand(
            Object command,
            List<MetricSample> metricBatch,
            List<AlertRecord> alertBatch,
            List<PluginProfileSnapshot> pluginBatch,
            List<EventProfileSnapshot> eventProfileBatch,
            List<WorldSnapshot> worldBatch,
            List<ChunkSnapshot> chunkBatch,
            List<AnalyzerFindingRecord> findingBatch
    ) {
        if (command instanceof MetricSample sample) {
            metricBatch.add(sample);
            return;
        }
        if (command instanceof AlertRecord alertRecord) {
            alertBatch.add(alertRecord);
            return;
        }
        if (command instanceof PluginProfileSnapshot snapshot) {
            pluginBatch.add(snapshot);
            return;
        }
        if (command instanceof EventProfileSnapshot snapshot) {
            eventProfileBatch.add(snapshot);
            return;
        }
        if (command instanceof WorldSnapshot snapshot) {
            worldBatch.add(snapshot);
            return;
        }
        if (command instanceof ChunkSnapshot snapshot) {
            chunkBatch.add(snapshot);
            return;
        }
        if (command instanceof AnalyzerFindingRecord record) {
            findingBatch.add(record);
            return;
        }
        throw new IllegalStateException("Unsupported storage command type " + command.getClass().getName());
    }

    private void drainAvailable(
            List<MetricSample> metricBatch,
            List<AlertRecord> alertBatch,
            List<PluginProfileSnapshot> pluginBatch,
            List<EventProfileSnapshot> eventProfileBatch,
            List<WorldSnapshot> worldBatch,
            List<ChunkSnapshot> chunkBatch,
            List<AnalyzerFindingRecord> findingBatch
    ) {
        while (totalBatchSize(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch) < config.maxBatchSize()) {
            Object command = queue.poll();
            if (command == null) {
                return;
            }
            handleCommand(command, metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
        }
    }

    private void flushIfNeeded(
            boolean idlePoll,
            List<MetricSample> metricBatch,
            List<AlertRecord> alertBatch,
            List<PluginProfileSnapshot> pluginBatch,
            List<EventProfileSnapshot> eventProfileBatch,
            List<WorldSnapshot> worldBatch,
            List<ChunkSnapshot> chunkBatch,
            List<AnalyzerFindingRecord> findingBatch
    ) throws SQLException {
        int batchSize = totalBatchSize(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
        if (batchSize == 0) {
            flushRequested = false;
            return;
        }

        if (flushRequested || stopRequested || batchSize >= config.maxBatchSize() || idlePoll) {
            flushBatches(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch);
            flushRequested = false;
        }
    }

    private int totalBatchSize(
            List<MetricSample> metricBatch,
            List<AlertRecord> alertBatch,
            List<PluginProfileSnapshot> pluginBatch,
            List<EventProfileSnapshot> eventProfileBatch,
            List<WorldSnapshot> worldBatch,
            List<ChunkSnapshot> chunkBatch,
            List<AnalyzerFindingRecord> findingBatch
    ) {
        return metricBatch.size()
                + alertBatch.size()
                + pluginBatch.size()
                + eventProfileBatch.size()
                + worldBatch.size()
                + chunkBatch.size()
                + findingBatch.size();
    }

    private void flushBatches(
            List<MetricSample> metricBatch,
            List<AlertRecord> alertBatch,
            List<PluginProfileSnapshot> pluginBatch,
            List<EventProfileSnapshot> eventProfileBatch,
            List<WorldSnapshot> worldBatch,
            List<ChunkSnapshot> chunkBatch,
            List<AnalyzerFindingRecord> findingBatch
    ) throws SQLException {
        if (totalBatchSize(metricBatch, alertBatch, pluginBatch, eventProfileBatch, worldBatch, chunkBatch, findingBatch) == 0) {
            return;
        }

        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                metricRepository.insertBatch(connection, metricBatch);
                alertRepository.insertBatch(connection, alertBatch);
                pluginProfileRepository.insertBatch(connection, pluginBatch);
                eventProfileSnapshotRepository.insertBatch(connection, eventProfileBatch);
                worldSnapshotRepository.insertBatch(connection, worldBatch);
                chunkSnapshotRepository.insertBatch(connection, chunkBatch);
                analyzerFindingRepository.insertBatch(connection, findingBatch);
                connection.commit();
                metricBatch.clear();
                alertBatch.clear();
                pluginBatch.clear();
                eventProfileBatch.clear();
                worldBatch.clear();
                chunkBatch.clear();
                findingBatch.clear();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void runRetentionCleanup() throws SQLException {
        Instant cutoff = Instant.now().minus(config.retentionDays(), ChronoUnit.DAYS);
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                metricRepository.deleteOlderThan(connection, cutoff);
                alertRepository.deleteOlderThan(connection, cutoff);
                pluginProfileRepository.deleteOlderThan(connection, cutoff);
                eventProfileSnapshotRepository.deleteOlderThan(connection, cutoff);
                worldSnapshotRepository.deleteOlderThan(connection, cutoff);
                chunkSnapshotRepository.deleteOlderThan(connection, cutoff);
                analyzerFindingRepository.deleteOlderThan(connection, cutoff);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void awaitInitialization() {
        try {
            if (!initialized.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting for SQLite initialization");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for SQLite initialization", exception);
        }
    }

    private void awaitInitializationOrThrow() {
        awaitInitialization();
        Throwable failure = initializationFailure.get();
        if (failure != null) {
            throw new IllegalStateException("SQLite storage initialization failed", failure);
        }
    }
}
