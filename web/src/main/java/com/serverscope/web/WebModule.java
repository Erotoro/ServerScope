package com.serverscope.web;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.lifecycle.AbstractManagedComponent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class WebModule extends AbstractManagedComponent {
    private final Logger logger;
    private final WebConfig config;
    private final TranslationService translations;
    private final ServerScopeHttpServer httpServer;

    public WebModule(
            Logger logger,
            WebConfig config,
            TranslationService translations,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<AlertRecord>> activeAlertsSupplier,
            Supplier<List<DiagnosticFinding>> activeFindingsSupplier
    ) {
        super("web");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
        this.httpServer = new ServerScopeHttpServer(
                logger,
                config,
                this.translations,
                Objects.requireNonNull(batchesSupplier, "batchesSupplier"),
                Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier"),
                Objects.requireNonNull(activeAlertsSupplier, "activeAlertsSupplier"),
                Objects.requireNonNull(activeFindingsSupplier, "activeFindingsSupplier")
        );
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
            updateHealth(ComponentStatus.FAILED, translations.text("web.health.failed"));
            throw exception;
        }
    }

    @Override
    public void stop() {
        httpServer.stop();
        logger.info(translations.text("web.log.stopped"));
        updateHealth(ComponentStatus.STOPPED, translations.text("web.health.stopped"));
    }
}
