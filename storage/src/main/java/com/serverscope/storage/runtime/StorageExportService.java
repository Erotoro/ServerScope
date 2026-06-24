package com.serverscope.storage.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.StorageConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.diagnostic.FindingSeverity;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import com.serverscope.api.profile.PluginProfileRecord;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.api.storage.AnalyzerFindingRecord;
import com.serverscope.api.storage.AnalyzerFindingSeverity;
import com.serverscope.api.storage.ChunkSnapshot;
import com.serverscope.api.storage.EventProfileSnapshot;
import com.serverscope.api.storage.StorageService;
import com.serverscope.api.storage.WorldSnapshot;
import com.serverscope.core.concurrent.NamedThreadFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StorageExportService {
    private static final String AGGREGATE_EVENT = "__aggregate__";
    private static final String AGGREGATE_LISTENER = "__approximate__";

    private final Logger logger;
    private final StorageConfig config;
    private final StorageService storageService;
    private final Supplier<Map<String, MetricBatch>> metricBatchesSupplier;
    private final Supplier<ProfilerSnapshot> profilerSnapshotSupplier;
    private final Supplier<List<AlertRecord>> activeAlertsSupplier;
    private final Supplier<List<DiagnosticFinding>> activeFindingsSupplier;
    private final ObjectMapper objectMapper;
    private final Map<String, Instant> lastWorldSamples = new HashMap<>();
    private final Map<String, Instant> lastChunkSamples = new HashMap<>();
    private final Map<String, Instant> lastPluginProfiles = new HashMap<>();
    private final Map<String, Instant> lastEventProfiles = new HashMap<>();
    private final Map<String, String> lastAlertFingerprints = new HashMap<>();
    private final Map<String, String> lastFindingFingerprints = new HashMap<>();

    private volatile ScheduledExecutorService executor;
    private volatile Instant lastAggregateMetricAt;

    public StorageExportService(
            Logger logger,
            StorageConfig config,
            StorageService storageService,
            Supplier<Map<String, MetricBatch>> metricBatchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<AlertRecord>> activeAlertsSupplier,
            Supplier<List<DiagnosticFinding>> activeFindingsSupplier
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.storageService = Objects.requireNonNull(storageService, "storageService");
        this.metricBatchesSupplier = Objects.requireNonNull(metricBatchesSupplier, "metricBatchesSupplier");
        this.profilerSnapshotSupplier = Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier");
        this.activeAlertsSupplier = Objects.requireNonNull(activeAlertsSupplier, "activeAlertsSupplier");
        this.activeFindingsSupplier = Objects.requireNonNull(activeFindingsSupplier, "activeFindingsSupplier");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void start() {
        if (!config.enabled()) {
            return;
        }
        if (executor != null) {
            return;
        }

        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                NamedThreadFactory.daemon("serverscope-storage-exporter"));
        scheduledExecutor.scheduleWithFixedDelay(
                this::safeExportOnce,
                config.flushIntervalMillis(),
                config.flushIntervalMillis(),
                TimeUnit.MILLISECONDS
        );
        executor = scheduledExecutor;
    }

    public void stop() {
        ScheduledExecutorService scheduledExecutor = executor;
        executor = null;
        if (scheduledExecutor == null) {
            return;
        }

        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            scheduledExecutor.shutdownNow();
            logger.log(Level.WARNING, "Interrupted while stopping storage exporter", exception);
        }

        safeExportOnce();
        storageService.flush();
    }

    private void safeExportOnce() {
        try {
            exportOnce();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Storage export cycle failed", exception);
        }
    }

    private void exportOnce() {
        Map<String, MetricBatch> batches = Map.copyOf(metricBatchesSupplier.get());
        ProfilerSnapshot profilerSnapshot = profilerSnapshotSupplier.get();
        List<AlertRecord> activeAlerts = List.copyOf(activeAlertsSupplier.get());
        List<DiagnosticFinding> activeFindings = List.copyOf(activeFindingsSupplier.get());

        exportAggregateMetricSample(batches);
        exportWorldSnapshots(batches);
        exportChunkSnapshots(batches);
        exportProfilerSnapshots(profilerSnapshot);
        exportAlertRecords(activeAlerts);
        exportAnalyzerFindings(activeFindings);
        storageService.flush();
    }

    private void exportAggregateMetricSample(Map<String, MetricBatch> batches) {
        Optional<MetricSample> tps = latestServerMetric(batches, MetricType.SERVER_TPS);
        Optional<MetricSample> mspt = latestServerMetric(batches, MetricType.SERVER_MSPT);
        Optional<MetricSample> heapUsed = latestServerMetric(batches, MetricType.JVM_HEAP_USED_BYTES);
        Optional<MetricSample> players = latestServerMetric(batches, MetricType.PLAYERS_ONLINE);
        Optional<MetricSample> loadedChunks = latestServerMetric(batches, MetricType.LOADED_CHUNKS);
        Optional<MetricSample> totalEntities = latestServerMetric(batches, MetricType.ENTITY_COUNT);

        if (tps.isEmpty() || mspt.isEmpty() || heapUsed.isEmpty() || players.isEmpty()) {
            return;
        }

        Instant sampleTime = StreamSupport.maxTimestamp(List.of(
                tps.get().timestamp(),
                mspt.get().timestamp(),
                heapUsed.get().timestamp(),
                players.get().timestamp(),
                loadedChunks.map(MetricSample::timestamp).orElse(tps.get().timestamp()),
                totalEntities.map(MetricSample::timestamp).orElse(tps.get().timestamp())
        ));
        if (lastAggregateMetricAt != null && !sampleTime.isAfter(lastAggregateMetricAt)) {
            return;
        }

        Map<String, WorldSnapshot> worldSnapshots = latestWorldSnapshotMap(batches);
        long worldCount = worldSnapshots.size();
        long totalLoadedChunks = loadedChunks
                .map(MetricSample::longValue)
                .orElseGet(() -> worldSnapshots.values().stream().mapToLong(WorldSnapshot::loadedChunks).sum());

        com.serverscope.api.storage.MetricSample aggregateSample = new com.serverscope.api.storage.MetricSample(
                sampleTime,
                tps.get().numericValue(),
                mspt.get().numericValue(),
                heapUsed.get().longValue(),
                (int) players.get().longValue(),
                (int) worldCount,
                totalLoadedChunks,
                totalEntities.map(MetricSample::longValue).orElse(0L)
        );
        if (storageService.enqueueMetricSample(aggregateSample)) {
            lastAggregateMetricAt = sampleTime;
        }
    }

    private void exportWorldSnapshots(Map<String, MetricBatch> batches) {
        for (WorldSnapshot snapshot : latestWorldSnapshotMap(batches).values()) {
            Instant previous = lastWorldSamples.get(snapshot.worldName());
            if (previous != null && !snapshot.sampleTime().isAfter(previous)) {
                continue;
            }
            if (storageService.enqueueWorldSnapshot(snapshot)) {
                lastWorldSamples.put(snapshot.worldName(), snapshot.sampleTime());
            }
        }
    }

    private Map<String, WorldSnapshot> latestWorldSnapshotMap(Map<String, MetricBatch> batches) {
        Map<String, WorldSnapshot> result = new HashMap<>();
        for (MetricBatch batch : batches.values()) {
            for (MetricSample sample : batch.samples()) {
                if (sample.type() != MetricType.LOADED_CHUNKS) {
                    continue;
                }
                String world = sample.labels().get("world");
                if (world == null || world.isBlank()) {
                    continue;
                }
                WorldSnapshot candidate = new WorldSnapshot(sample.timestamp(), world, sample.longValue());
                result.merge(world, candidate, (left, right) ->
                        right.sampleTime().isAfter(left.sampleTime()) ? right : left);
            }
        }
        return result;
    }

    private void exportChunkSnapshots(Map<String, MetricBatch> batches) {
        Map<String, MetricSample> latestEntitySamples = new HashMap<>();
        Map<String, MetricSample> latestBlockEntitySamples = new HashMap<>();

        for (MetricBatch batch : batches.values()) {
            for (MetricSample sample : batch.samples()) {
                String key = chunkKey(sample);
                if (key == null) {
                    continue;
                }
                if (sample.type() == MetricType.ENTITY_COUNT) {
                    latestEntitySamples.merge(key, sample, StorageExportService::latestByTimestamp);
                } else if (sample.type() == MetricType.BLOCK_ENTITY_COUNT) {
                    latestBlockEntitySamples.merge(key, sample, StorageExportService::latestByTimestamp);
                }
            }
        }

        Set<String> keys = new TreeSet<>();
        keys.addAll(latestEntitySamples.keySet());
        keys.addAll(latestBlockEntitySamples.keySet());

        for (String key : keys) {
            MetricSample entitySample = latestEntitySamples.get(key);
            MetricSample blockEntitySample = latestBlockEntitySamples.get(key);
            if (entitySample == null && blockEntitySample == null) {
                continue;
            }

            MetricSample reference = entitySample != null ? entitySample : blockEntitySample;
            Integer chunkX = parseChunkCoordinate(reference.labels().get("chunk_x"));
            Integer chunkZ = parseChunkCoordinate(reference.labels().get("chunk_z"));
            if (chunkX == null || chunkZ == null) {
                continue;
            }
            Instant previous = lastChunkSamples.get(key);
            if (previous != null && !reference.timestamp().isAfter(previous)) {
                continue;
            }

            String world = reference.labels().get("world");
            int entityCount = entitySample != null ? (int) entitySample.longValue() : 0;
            int blockEntityCount = blockEntitySample != null ? (int) blockEntitySample.longValue() : 0;
            long hotspotScore = entityCount * 10L + blockEntityCount * 25L;

            ChunkSnapshot snapshot = new ChunkSnapshot(
                    reference.timestamp(),
                    world,
                    chunkX,
                    chunkZ,
                    entityCount,
                    blockEntityCount,
                    hotspotScore
            );
            if (storageService.enqueueChunkSnapshot(snapshot)) {
                lastChunkSamples.put(key, reference.timestamp());
            }
        }
    }

    private void exportProfilerSnapshots(ProfilerSnapshot profilerSnapshot) {
        if (profilerSnapshot == null) {
            return;
        }

        for (PluginProfileRecord record : profilerSnapshot.topPlugins()) {
            Instant previous = lastPluginProfiles.get(record.pluginName());
            if (previous != null && !profilerSnapshot.capturedAt().isAfter(previous)) {
                continue;
            }
            com.serverscope.api.storage.PluginProfileSnapshot snapshot = new com.serverscope.api.storage.PluginProfileSnapshot(
                    profilerSnapshot.capturedAt(),
                    record.pluginName(),
                    AGGREGATE_EVENT,
                    AGGREGATE_LISTENER,
                    record.eventCount(),
                    record.attributedTotalTimeNanos(),
                    record.maxAttributedTimeNanos(),
                    record.averageAttributedTimeNanos()
            );
            if (storageService.enqueuePluginProfileSnapshot(snapshot)) {
                lastPluginProfiles.put(record.pluginName(), profilerSnapshot.capturedAt());
            }
        }

        exportEventProfileSnapshots(profilerSnapshot.topSlowEvents(), "SLOW", profilerSnapshot.capturedAt());
        exportEventProfileSnapshots(profilerSnapshot.topFrequentEvents(), "FREQUENT", profilerSnapshot.capturedAt());
        exportEventProfileSnapshots(profilerSnapshot.topSuspiciousBursts(), "BURST", profilerSnapshot.capturedAt());
    }

    private void exportEventProfileSnapshots(List<com.serverscope.api.profile.EventProfileRecord> records, String kind, Instant capturedAt) {
        for (com.serverscope.api.profile.EventProfileRecord record : records) {
            String key = kind + ":" + record.eventId();
            Instant previous = lastEventProfiles.get(key);
            if (previous != null && !capturedAt.isAfter(previous)) {
                continue;
            }

            EventProfileSnapshot snapshot = new EventProfileSnapshot(
                    capturedAt,
                    kind,
                    record.eventId(),
                    record.eventClassName(),
                    record.count(),
                    record.totalTimeNanos(),
                    record.maxTimeNanos(),
                    record.averageTimeNanos(),
                    record.maxWindowCount(),
                    record.burstScore()
            );
            if (storageService.enqueueEventProfileSnapshot(snapshot)) {
                lastEventProfiles.put(key, capturedAt);
            }
        }
    }

    private void exportAlertRecords(List<AlertRecord> activeAlerts) {
        for (AlertRecord alert : activeAlerts) {
            String fingerprint = alert.status() + "|" + alert.severity() + "|" + alert.message();
            if (fingerprint.equals(lastAlertFingerprints.get(alert.dedupeKey()))) {
                continue;
            }
            com.serverscope.api.storage.AlertRecord storageAlert = new com.serverscope.api.storage.AlertRecord(
                    alert.occurredAt(),
                    alert.code(),
                    com.serverscope.api.storage.AlertSeverity.valueOf(alert.severity().name()),
                    com.serverscope.api.storage.AlertStatus.valueOf(alert.status().name()),
                    alert.dedupeKey(),
                    alert.message(),
                    toJson(alert.labels())
            );
            if (storageService.enqueueAlert(storageAlert)) {
                lastAlertFingerprints.put(alert.dedupeKey(), fingerprint);
            }
        }
    }

    private void exportAnalyzerFindings(List<DiagnosticFinding> activeFindings) {
        for (DiagnosticFinding finding : activeFindings) {
            String subject = finding.reference() == null
                    ? finding.id()
                    : referenceSubject(finding);
            enqueueFindingIfChanged(
                    finding.id(),
                    new AnalyzerFindingRecord(
                            finding.timestamp(),
                            finding.id(),
                            toFindingSeverity(finding.severity()),
                            subject,
                            finding.title(),
                            toJson(findingPayload(finding))
                    )
            );
        }
    }

    private Map<String, Object> findingPayload(DiagnosticFinding finding) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", finding.title());
        payload.put("description", finding.description());
        payload.put("probableCause", finding.probableCause());
        payload.put("suggestedAction", finding.suggestedAction());
        payload.put("confidence", finding.confidence() == null ? null : finding.confidence().name());
        payload.put("reference", finding.reference());
        return payload;
    }

    private void enqueueFindingIfChanged(String key, AnalyzerFindingRecord record) {
        String fingerprint = record.severity() + "|" + record.subject() + "|" + record.message();
        if (fingerprint.equals(lastFindingFingerprints.get(key))) {
            return;
        }
        if (storageService.enqueueAnalyzerFinding(record)) {
            lastFindingFingerprints.put(key, fingerprint);
        }
    }

    private Optional<MetricSample> latestServerMetric(Map<String, MetricBatch> batches, MetricType type) {
        return batches.values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type() == type)
                .filter(sample -> sample.labels().isEmpty())
                .max(Comparator.comparing(MetricSample::timestamp));
    }

    private static MetricSample latestByTimestamp(MetricSample left, MetricSample right) {
        return right.timestamp().isAfter(left.timestamp()) ? right : left;
    }

    private static String chunkKey(MetricSample sample) {
        String world = sample.labels().get("world");
        String chunkX = sample.labels().get("chunk_x");
        String chunkZ = sample.labels().get("chunk_z");
        if (world == null || chunkX == null || chunkZ == null) {
            return null;
        }
        return world + ":" + chunkX + ":" + chunkZ;
    }

    private Integer parseChunkCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            logger.fine("Skipping chunk snapshot export with invalid chunk coordinate: " + value);
            return null;
        }
    }

    private AnalyzerFindingSeverity toFindingSeverity(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> AnalyzerFindingSeverity.CRITICAL;
            case WARN -> AnalyzerFindingSeverity.WARN;
            case INFO -> AnalyzerFindingSeverity.INFO;
        };
    }

    private String referenceSubject(DiagnosticFinding finding) {
        if (finding.reference() == null) {
            return finding.id();
        }
        if (finding.reference().pluginName() != null && !finding.reference().pluginName().isBlank()) {
            return finding.reference().pluginName();
        }
        if (finding.reference().worldName() != null && finding.reference().chunkX() != null && finding.reference().chunkZ() != null) {
            return "%s:%d:%d".formatted(finding.reference().worldName(), finding.reference().chunkX(), finding.reference().chunkZ());
        }
        if (finding.reference().worldName() != null && !finding.reference().worldName().isBlank()) {
            return finding.reference().worldName();
        }
        return finding.id();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize storage export payload", exception);
        }
    }
    private static final class StreamSupport {
        private StreamSupport() {
        }

        private static Instant maxTimestamp(List<Instant> timestamps) {
            return timestamps.stream().max(Comparator.naturalOrder()).orElse(Instant.EPOCH);
        }
    }
}
