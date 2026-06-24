package com.serverscope.web;

import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.core.i18n.TranslationService;
import org.junit.jupiter.api.Test;

import java.util.logging.Handler;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebModuleTest {
    @Test
    void startMarksModuleFailedWhenEmbeddedServerCannotStart() {
        WebModule module = new WebModule(
                quietLogger("test.web.module"),
                new WebConfig(true, "127.0.0.1", 8080, "secret-token", false, "", false, 60, 10_000L, 2048, 1_048_576),
                new TranslationService(quietLogger("test.web.module.i18n"), new LocalizationConfig("en", false)),
                new FailingWebServer()
        );

        assertDoesNotThrow(module::start);
        assertEquals(ComponentStatus.FAILED, module.health().status());
        assertTrue(module.toleratesDegradedStartup());
    }

    private static final class FailingWebServer implements WebServerControl {
        @Override
        public void start() {
            throw new IllegalStateException("bind failed");
        }

        @Override
        public void stop() {
        }
    }

    private static Logger quietLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        return logger;
    }
}
