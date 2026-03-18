package com.serverscope.analyzer.alert;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.alert.AlertSeverity;
import com.serverscope.api.alert.AlertStatus;
import com.serverscope.api.config.AlertThresholdsConfig;
import com.serverscope.api.config.AlertingConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.diagnostic.DiagnosticReference;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import com.serverscope.api.profile.EventProfileRecord;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.core.i18n.TranslationService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AlertEvaluationService {
    private final Logger logger;
    private final AlertingConfig config;
    private final TranslationService translations;
    private final AlertDispatcher dispatcher;
    private final Supplier<Map<String, MetricBatch>> batchesSupplier;
    private final Supplier<ProfilerSnapshot> profilerSnapshotSupplier;
    private final Supplier<List<DiagnosticFinding>> findingsSupplier;
    private final ConcurrentMap<String, AlertState> states = new ConcurrentHashMap<>();

    public AlertEvaluationService(
            Logger logger,
            AlertingConfig config,
            TranslationService translations,
            AlertDispatcher dispatcher,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<DiagnosticFinding>> findingsSupplier
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.batchesSupplier = Objects.requireNonNull(batchesSupplier, "batchesSupplier");
        this.profilerSnapshotSupplier = Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier");
        this.findingsSupplier = Objects.requireNonNull(findingsSupplier, "findingsSupplier");
    }

    public void evaluateOnce() {
        try {
            Map<String, AlertRecord> current = evaluateCurrentAlerts();
            reconcile(current);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Alert evaluation failed", exception);
        }
    }

    public List<AlertRecord> activeAlerts() {
        return states.values().stream()
                .filter(AlertState::active)
                .map(AlertState::record)
                .sorted(Comparator.comparing(AlertRecord::occurredAt).reversed())
                .toList();
    }

    private Map<String, AlertRecord> evaluateCurrentAlerts() {
        Map<String, MetricBatch> batches = batchesSupplier.get();
        ProfilerSnapshot profilerSnapshot = profilerSnapshotSupplier.get();
        Map<String, AlertRecord> current = new HashMap<>();
        AlertThresholdsConfig thresholds = config.thresholds();
        Instant now = Instant.now();

        latestSample(batches, MetricType.SERVER_TPS).ifPresent(sample -> {
            if (sample.numericValue() < thresholds.lowTps()) {
                put(current, new AlertRecord(
                        "LOW_TPS",
                        "LOW_TPS:server",
                        AlertSeverity.CRITICAL,
                        AlertStatus.ACTIVE,
                        translations.text("alert.metric.low_tps.active", Map.of("value", sample.numericValue())),
                        now,
                        Map.of("scope", "server")
                ));
            }
        });

        latestSample(batches, MetricType.SERVER_MSPT).ifPresent(sample -> {
            if (sample.numericValue() > thresholds.highMspt()) {
                put(current, new AlertRecord(
                        "HIGH_MSPT",
                        "HIGH_MSPT:server",
                        AlertSeverity.CRITICAL,
                        AlertStatus.ACTIVE,
                        translations.text("alert.metric.high_mspt.active", Map.of("value", sample.numericValue())),
                        now,
                        Map.of("scope", "server")
                ));
            }
        });

        latestSample(batches, MetricType.ENTITY_COUNT)
                .filter(sample -> sample.labels().isEmpty())
                .ifPresent(sample -> {
                    if (sample.longValue() > thresholds.highEntityCount()) {
                        put(current, new AlertRecord(
                                "HIGH_ENTITY_COUNT",
                                "HIGH_ENTITY_COUNT:server",
                                AlertSeverity.WARN,
                                AlertStatus.ACTIVE,
                                translations.text("alert.metric.high_entity_count.active", Map.of("value", sample.longValue())),
                                now,
                                Map.of("scope", "server")
                        ));
                    }
                });

        latestChunkSample(batches, MetricType.ENTITY_COUNT).ifPresent(sample -> {
            if (sample.longValue() > thresholds.highChunkEntityCount()) {
                put(current, new AlertRecord(
                        "HOT_CHUNK_ENTITIES",
                        "HOT_CHUNK_ENTITIES:%s:%s:%s".formatted(
                                sample.labels().get("world"),
                                sample.labels().get("chunk_x"),
                                sample.labels().get("chunk_z")
                        ),
                        AlertSeverity.WARN,
                        AlertStatus.ACTIVE,
                        translations.text("alert.metric.hot_chunk_entities.active", Map.of(
                                "chunkX", sample.labels().get("chunk_x"),
                                "chunkZ", sample.labels().get("chunk_z"),
                                "world", sample.labels().get("world"),
                                "value", sample.longValue()
                        )),
                        now,
                        sample.labels().values()
                ));
            }
        });

        latestChunkSample(batches, MetricType.BLOCK_ENTITY_COUNT).ifPresent(sample -> {
            if (sample.longValue() > thresholds.highChunkBlockEntityCount()) {
                put(current, new AlertRecord(
                        "HOT_CHUNK_BLOCK_ENTITIES",
                        "HOT_CHUNK_BLOCK_ENTITIES:%s:%s:%s".formatted(
                                sample.labels().get("world"),
                                sample.labels().get("chunk_x"),
                                sample.labels().get("chunk_z")
                        ),
                        AlertSeverity.WARN,
                        AlertStatus.ACTIVE,
                        translations.text("alert.metric.hot_chunk_block_entities.active", Map.of(
                                "chunkX", sample.labels().get("chunk_x"),
                                "chunkZ", sample.labels().get("chunk_z"),
                                "world", sample.labels().get("world"),
                                "value", sample.longValue()
                        )),
                        now,
                        sample.labels().values()
                ));
            }
        });

        if (profilerSnapshot != null) {
            for (EventProfileRecord record : profilerSnapshot.topSlowEvents()) {
                double avgMillis = record.averageTimeNanos() / 1_000_000.0d;
                if (avgMillis > thresholds.highEventAverageMillis()) {
                    put(current, new AlertRecord(
                            "SLOW_EVENT",
                            "SLOW_EVENT:" + record.eventId(),
                            AlertSeverity.WARN,
                            AlertStatus.ACTIVE,
                            translations.text("alert.metric.slow_event.active", Map.of(
                                    "eventId", record.eventId(),
                                    "value", avgMillis
                            )),
                            now,
                            Map.of("event", record.eventId())
                    ));
                }
            }
        }

        for (DiagnosticFinding finding : findingsSupplier.get()) {
            toAlertFromFinding(finding, now).ifPresent(alert -> put(current, alert));
        }

        return current;
    }

    private void reconcile(Map<String, AlertRecord> current) {
        Instant now = Instant.now();

        for (AlertRecord alert : current.values()) {
            AlertState state = states.computeIfAbsent(alert.dedupeKey(), ignored -> new AlertState());
            if (!state.active()) {
                state.activate(alert);
                dispatch(state, alert);
                continue;
            }

            boolean changed = state.isChanged(alert);
            state.refresh(alert);
            if (changed) {
                dispatch(state, alert);
            }
        }

        List<String> resolvedKeys = new ArrayList<>();
        for (Map.Entry<String, AlertState> entry : states.entrySet()) {
            if (current.containsKey(entry.getKey())) {
                continue;
            }
            AlertState state = entry.getValue();
            if (!state.active()) {
                continue;
            }

            AlertRecord resolved = new AlertRecord(
                    state.record().code(),
                    state.record().dedupeKey(),
                    state.record().severity(),
                    AlertStatus.RESOLVED,
                    translations.text("alert.common.resolved", Map.of("message", state.record().message())),
                    now,
                    state.record().labels()
            );
            state.resolve(resolved);
            dispatch(state, resolved);
            resolvedKeys.add(entry.getKey());
        }

        for (String resolvedKey : resolvedKeys) {
            states.remove(resolvedKey);
        }
    }

    private void dispatch(AlertState state, AlertRecord alert) {
        dispatcher.dispatch(alert);
    }

    private void put(Map<String, AlertRecord> current, AlertRecord alert) {
        current.put(alert.dedupeKey(), alert);
    }

    private java.util.Optional<AlertRecord> toAlertFromFinding(DiagnosticFinding finding, Instant now) {
        if (finding.id().startsWith("server.health.tick-degradation")
                || finding.id().startsWith("chunk.hotspot.")
                || finding.id().startsWith("server.entities.spike")) {
            return java.util.Optional.empty();
        }

        AlertSeverity severity = switch (finding.severity()) {
            case CRITICAL -> AlertSeverity.CRITICAL;
            case WARN -> AlertSeverity.WARN;
            case INFO -> AlertSeverity.INFO;
        };
        String code = alertCodeForFinding(finding.id());
        return java.util.Optional.of(new AlertRecord(
                code,
                "FINDING:" + finding.id(),
                severity,
                AlertStatus.ACTIVE,
                alertMessageForFinding(finding),
                now,
                labelsForFinding(finding)
        ));
    }

    private String alertCodeForFinding(String findingId) {
        if (findingId.startsWith("server.memory.pressure")) {
            return "MEMORY_PRESSURE";
        }
        if (findingId.startsWith("event.burst.")) {
            return "EVENT_BURST";
        }
        if (findingId.startsWith("server.chunks.spike")) {
            return "LOADED_CHUNK_SPIKE";
        }
        if (findingId.startsWith("server.health.unstable-trend")) {
            return "UNSTABLE_SERVER_TREND";
        }
        return "DIAGNOSTIC_FINDING";
    }

    private String alertMessageForFinding(DiagnosticFinding finding) {
        String findingId = finding.id();
        if (findingId.startsWith("server.chunks.spike")) {
            return translations.text("alert.finding.loaded_chunk_spike.active");
        }
        if (findingId.startsWith("server.health.unstable-trend")) {
            return translations.text("alert.finding.unstable_server_trend.active");
        }
        if (findingId.startsWith("event.burst.")) {
            return translations.text("alert.finding.event_burst.active");
        }
        if (findingId.startsWith("server.memory.pressure")) {
            return translations.text("alert.finding.memory_pressure.active");
        }
        return finding.title();
    }

    private Map<String, String> labelsForFinding(DiagnosticFinding finding) {
        Map<String, String> labels = new HashMap<>();
        labels.put("findingId", finding.id());
        labels.put("confidence", finding.confidence().name());
        DiagnosticReference reference = finding.reference();
        if (reference == null) {
            return Map.copyOf(labels);
        }
        if (reference.worldName() != null) {
            labels.put("world", reference.worldName());
        }
        if (reference.chunkX() != null) {
            labels.put("chunk_x", Integer.toString(reference.chunkX()));
        }
        if (reference.chunkZ() != null) {
            labels.put("chunk_z", Integer.toString(reference.chunkZ()));
        }
        if (reference.pluginName() != null) {
            labels.put("plugin", reference.pluginName());
        }
        return Map.copyOf(labels);
    }

    private java.util.Optional<MetricSample> latestSample(Map<String, MetricBatch> batches, MetricType type) {
        return batches.values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type() == type)
                .max(Comparator.comparing(MetricSample::timestamp));
    }

    private java.util.Optional<MetricSample> latestChunkSample(Map<String, MetricBatch> batches, MetricType type) {
        return batches.values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type() == type)
                .filter(sample -> !sample.labels().isEmpty() && sample.labels().get("chunk_x") != null)
                .max(Comparator.comparingDouble(MetricSample::numericValue));
    }

    private static final class AlertState {
        private volatile AlertRecord record;
        private volatile boolean active;

        boolean active() {
            return active;
        }

        AlertRecord record() {
            return record;
        }

        void activate(AlertRecord record) {
            this.record = record;
            this.active = true;
        }

        void refresh(AlertRecord record) {
            this.record = record;
        }

        boolean isChanged(AlertRecord candidate) {
            AlertRecord current = record;
            if (current == null) {
                return true;
            }
            return current.status() != candidate.status()
                    || current.severity() != candidate.severity()
                    || !current.message().equals(candidate.message());
        }

        void resolve(AlertRecord record) {
            this.record = record;
            this.active = false;
        }
    }
}
