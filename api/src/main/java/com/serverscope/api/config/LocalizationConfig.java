package com.serverscope.api.config;

import java.util.Objects;

public record LocalizationConfig(
        String defaultLocale,
        boolean logMissingTranslations
) {
    public LocalizationConfig {
        Objects.requireNonNull(defaultLocale, "defaultLocale");
    }
}
