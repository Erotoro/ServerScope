package com.serverscope.analyzer.alert;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.AlertChannelsConfig;

import java.util.Objects;
import java.util.logging.Logger;

public final class ConsoleAlertNotifier implements AlertNotifier {
    private final Logger logger;
    private final AlertChannelsConfig config;

    public ConsoleAlertNotifier(Logger logger, AlertChannelsConfig config) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void notify(AlertRecord alertRecord) {
        if (!config.consoleEnabled()) {
            return;
        }
        java.util.logging.Level level = switch (alertRecord.severity()) {
            case CRITICAL, WARN -> java.util.logging.Level.WARNING;
            case INFO -> java.util.logging.Level.INFO;
        };
        logger.log(level, "[ServerScope][{0}][{1}] {2} {3}", new Object[]{
                alertRecord.severity(),
                alertRecord.status(),
                alertRecord.code(),
                alertRecord.message()
        });
    }
}
