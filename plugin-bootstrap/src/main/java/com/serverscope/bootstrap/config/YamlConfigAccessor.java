package com.serverscope.bootstrap.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class YamlConfigAccessor {
    private final ConfigurationSection section;
    private final Logger logger;

    public YamlConfigAccessor(ConfigurationSection section, Logger logger) {
        this.section = Objects.requireNonNull(section, "section");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public boolean booleanValue(String path, boolean fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null) {
            warn(path, "boolean", fallback);
        }
        return fallback;
    }

    public String stringValue(String path, String fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        if (value != null) {
            warn(path, "non-empty string", fallback);
        }
        return fallback;
    }

    public int intValue(String path, int fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            warn(path, "integer", fallback);
        }
        return fallback;
    }

    public long longValue(String path, long fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            warn(path, "long", fallback);
        }
        return fallback;
    }

    public double doubleValue(String path, double fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            warn(path, "double", fallback);
        }
        return fallback;
    }

    public List<String> stringList(String path, List<String> fallback, String... legacyPaths) {
        Object value = find(path, legacyPaths);
        if (value instanceof List<?> list) {
            List<String> strings = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(entry -> !entry.isBlank())
                    .toList();
            if (!strings.isEmpty()) {
                return strings;
            }
        }
        if (value != null) {
            warn(path, "string list", fallback);
        }
        return fallback;
    }

    private Object find(String path, String... legacyPaths) {
        if (section.contains(path, true)) {
            return section.get(path);
        }
        for (String legacyPath : legacyPaths) {
            if (legacyPath != null && section.contains(legacyPath, true)) {
                logger.warning("Config key '" + legacyPath + "' is deprecated; use '" + path + "' instead.");
                return section.get(legacyPath);
            }
        }
        return null;
    }

    private void warn(String path, String expectedType, Object fallback) {
        logger.warning("Invalid config value at '" + path + "'. Expected " + expectedType + ", using fallback '" + fallback + "'.");
    }
}
