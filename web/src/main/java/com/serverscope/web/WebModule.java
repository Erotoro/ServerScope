package com.serverscope.web;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.api.storage.AlertRepository;
import com.serverscope.api.storage.MetricSampleRepository;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.lifecycle.AbstractManagedComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WebModule extends AbstractManagedComponent {
    private final Logger logger;
    private final WebConfig config;
    private final TranslationService translations;
    private final WebServerControl httpServer;

    public WebModule(
            Logger logger,
            WebConfig config,
            TranslationService translations,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<AlertRecord>> activeAlertsSupplier,
            Supplier<List<DiagnosticFinding>> activeFindingsSupplier,
            MetricSampleRepository historyRepository,
            AlertRepository alertHistoryRepository
    ) {
        this(logger,
                config,
                translations,
                new ServerScopeHttpServer(
                        logger,
                        config,
                        translations,
                        Objects.requireNonNull(batchesSupplier, "batchesSupplier"),
                        Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier"),
                        Objects.requireNonNull(activeAlertsSupplier, "activeAlertsSupplier"),
                        Objects.requireNonNull(activeFindingsSupplier, "activeFindingsSupplier"),
                        Objects.requireNonNull(historyRepository, "historyRepository"),
                        Objects.requireNonNull(alertHistoryRepository, "alertHistoryRepository")));
    }

    WebModule(
            Logger logger,
            WebConfig config,
            TranslationService translations,
            WebServerControl httpServer
    ) {
        super("web");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
        this.httpServer = Objects.requireNonNull(httpServer, "httpServer");
    }

    @Override
    public boolean toleratesDegradedStartup() {
        return true;
    }

    @Override
    public void start() {
        if (!config.enabled()) {
            logger.info(translations.text("web.log.disabled"));
            updateHealth(ComponentStatus.RUNNING, translations.text("web.health.disabled"));
            return;
        }
        try {
            httpServer.start();
            logger.info(translations.text("web.log.started", Map.of("host", config.host(), "port", config.port())));
            updateHealth(ComponentStatus.RUNNING, translations.text("web.health.active"));
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "ServerScope web panel failed to start; core monitoring remains available", exception);
            updateHealth(ComponentStatus.FAILED, translations.text("web.health.failed"));
        }
    }

    @Override
    public void stop() {
        httpServer.stop();
        logger.info(translations.text("web.log.stopped"));
        updateHealth(ComponentStatus.STOPPED, translations.text("web.health.stopped"));
    }
}
