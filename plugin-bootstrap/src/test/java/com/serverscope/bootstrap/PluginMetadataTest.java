package com.serverscope.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginMetadataTest {
    @Test
    void releaseFacingMetadataTargetsPaperAndFoliaOnly() throws Exception {
        try (InputStream stream = PluginMetadataTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream, "plugin.yml must be packaged as a test resource");
            String pluginYml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(pluginYml.contains("Paper/Folia"), pluginYml);
            assertFalse(pluginYml.contains("Spigot"), pluginYml);
        }

        String readme = Files.readString(Path.of("..", "README.md"), StandardCharsets.UTF_8);
        assertFalse(readme.contains("Spigot"), readme);
    }
}
