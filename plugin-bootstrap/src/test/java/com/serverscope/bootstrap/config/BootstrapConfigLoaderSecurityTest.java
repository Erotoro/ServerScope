package com.serverscope.bootstrap.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapConfigLoaderSecurityTest {
    @Test
    void placeholderAndBlankTokensTriggerAutogeneration() {
        assertTrue(BootstrapConfigLoader.shouldGenerateWebToken(null));
        assertTrue(BootstrapConfigLoader.shouldGenerateWebToken(""));
        assertTrue(BootstrapConfigLoader.shouldGenerateWebToken("change-me-serverscope-token"));
        assertTrue(BootstrapConfigLoader.shouldGenerateWebToken("auto-generated-per-server"));
    }

    @Test
    void privateWebhookUrlIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BootstrapConfigLoader.validateWebhookUrl(true, "https://127.0.0.1/hook"));
        assertThrows(IllegalArgumentException.class,
                () -> BootstrapConfigLoader.validateWebhookUrl(true, "https://localhost/hook"));
    }

    @Test
    void sqliteFileMustStayInsidePluginFolder() {
        assertThrows(IllegalArgumentException.class,
                () -> BootstrapConfigLoader.resolveSqliteFile(Path.of("C:/servers/plugins/ServerScope"), "../../outside.db"));
        assertDoesNotThrow(() -> BootstrapConfigLoader.resolveSqliteFile(Path.of("C:/servers/plugins/ServerScope"), "serverscope.db"));
    }
}
