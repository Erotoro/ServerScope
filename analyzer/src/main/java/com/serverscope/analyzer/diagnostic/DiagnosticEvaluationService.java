package com.serverscope.analyzer.diagnostic;

import com.serverscope.api.config.AlertingConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.core.i18n.TranslationService;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiagnosticEvaluationService {
    private static final int MAX_HISTORY_SAMPLES = 24;

    private final Logger logger;
    private final AlertingConfig alertingConfig;
    private final Supplier<Map<String, MetricBatch>> batchesSupplier;
    private final Supplier<ProfilerSnapshot> profilerSnapshotSupplier;
    private final MetricSnapshotExtractor extractor;
    private final List<DiagnosticRule> rules;
    private final Deque<ServerHealthSample> recentHealthSamples;
    private final AtomicReference<List<DiagnosticFinding>> activeFindings;

    public DiagnosticEvaluationService(
            Logger logger,
            AlertingConfig alertingConfig,
            TranslationService translations,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.alertingConfig = Objects.requireNonNull(alertingConfig, "alertingConfig");
        this.batchesSupplier = Objects.requireNonNull(batchesSupplier, "batchesSupplier");
        this.profilerSnapshotSupplier = Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier");
        this.extractor = new MetricSnapshotExtractor();
        this.rules = DiagnosticRules.defaults(Objects.requireNonNull(translations, "translations"));
        this.recentHealthSamples = new ArrayDeque<>();
        this.activeFindings = new AtomicReference<>(List.of());
    }

    public void evaluateOnce() {
        try {
            Map<String, MetricBatch> batches = batchesSupplier.get();
            ProfilerSnapshot profilerSnapshot = profilerSnapshotSupplier.get();
            var currentHealth = extractor.extractServerHealth(batches);
            currentHealth.ifPresent(this::rememberSample);

            RuleEvaluationContext context = new RuleEvaluationContext(
                    Instant.now(),
                    alertingConfig.thresholds(),
                    currentHealth,
                    List.copyOf(recentHealthSamples),
                    extractor.extractWorldLoadedChunks(batches),
                    extractor.extractHotChunks(batches, 8),
                    profilerSnapshot
            );

            List<DiagnosticFinding> findings = rules.stream()
                    .map(rule -> rule.evaluate(context))
                    .flatMap(java.util.Optional::stream)
                    .sorted(Comparator.comparing(DiagnosticFinding::severity).reversed()
                            .thenComparing(DiagnosticFinding::timestamp).reversed())
                    .toList();
            activeFindings.set(findings);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Diagnostic evaluation failed", exception);
        }
    }

    public List<DiagnosticFinding> activeFindings() {
        return activeFindings.get();
    }

    private void rememberSample(ServerHealthSample sample) {
        if (!recentHealthSamples.isEmpty()) {
            ServerHealthSample last = recentHealthSamples.peekLast();
            if (last != null && !sample.timestamp().isAfter(last.timestamp())) {
                return;
            }
        }
        recentHealthSamples.addLast(sample);
        while (recentHealthSamples.size() > MAX_HISTORY_SAMPLES) {
            recentHealthSamples.removeFirst();
        }
    }
}
