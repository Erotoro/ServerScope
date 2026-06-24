package com.serverscope.analyzer;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.alert.AlertService;
import com.serverscope.api.config.AlertingConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.diagnostic.DiagnosticService;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.analyzer.alert.AlertDispatcher;
import com.serverscope.analyzer.alert.AlertEvaluationService;
import com.serverscope.analyzer.alert.AlertNotifier;
import com.serverscope.analyzer.alert.ConsoleAlertNotifier;
import com.serverscope.analyzer.alert.InGameAdminAlertNotifier;
import com.serverscope.analyzer.alert.WebhookAlertNotifier;
import com.serverscope.analyzer.diagnostic.DiagnosticEvaluationService;
import com.serverscope.core.concurrent.NamedThreadFactory;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.lifecycle.AbstractManagedComponent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class AnalyzerModule extends AbstractManagedComponent implements AlertService, DiagnosticService {
    private static final long STOP_TIMEOUT_SECONDS = 5L;

    private final Logger logger;
    private final AlertingConfig config;
    private final TranslationService translations;
    private final DiagnosticEvaluationService diagnosticEvaluationService;
    private final AlertEvaluationService evaluationService;
    private final ScheduledExecutorService executorService;

    public AnalyzerModule(
            JavaPlugin plugin,
            Logger logger,
            AlertingConfig config,
            TranslationService translations,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier
    ) {
        super("analyzer");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
        this.diagnosticEvaluationService = new DiagnosticEvaluationService(
                logger,
                config,
                this.translations,
                Objects.requireNonNull(batchesSupplier, "batchesSupplier"),
                Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier")
        );

        List<AlertNotifier> notifiers = List.of(
                new ConsoleAlertNotifier(logger, config.channels()),
                new InGameAdminAlertNotifier(plugin, config.channels(), this.translations),
                new WebhookAlertNotifier(logger, config.channels())
        );
        this.evaluationService = new AlertEvaluationService(
                logger,
                config,
                this.translations,
                new AlertDispatcher(config, notifiers),
                batchesSupplier,
                profilerSnapshotSupplier,
                this.diagnosticEvaluationService::activeFindings
        );
        this.executorService = Executors.newSingleThreadScheduledExecutor(
                NamedThreadFactory.daemon("serverscope-analyzer"));
    }

    @Override
    public void start() {
        executorService.scheduleAtFixedRate(
                this::runAnalysisCycle,
                config.evaluationIntervalMillis(),
                config.evaluationIntervalMillis(),
                TimeUnit.MILLISECONDS
        );
        logger.info(translations.text("analyzer.log.started"));
        updateHealth(ComponentStatus.RUNNING, config.enabled()
                ? translations.text("analyzer.health.active")
                : translations.text("analyzer.health.notifications_disabled"));
    }

    @Override
    public void stop() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            logger.warning(translations.text("analyzer.log.stop_interrupted"));
        }
        logger.info(translations.text("analyzer.log.stopped"));
        updateHealth(ComponentStatus.STOPPED, translations.text("analyzer.health.stopped"));
    }

    @Override
    public List<AlertRecord> activeAlerts() {
        if (!config.enabled()) {
            return List.of();
        }
        return evaluationService.activeAlerts();
    }

    @Override
    public List<DiagnosticFinding> activeFindings() {
        return diagnosticEvaluationService.activeFindings();
    }

    private void runAnalysisCycle() {
        diagnosticEvaluationService.evaluateOnce();
        if (config.enabled()) {
            evaluationService.evaluateOnce();
        }
    }
}
