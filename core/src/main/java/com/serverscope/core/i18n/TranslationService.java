package com.serverscope.core.i18n;

import com.serverscope.api.config.LocalizationConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TranslationService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_.-]+)}");
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "ru", "uk");
    private static final String FALLBACK_LOCALE = "en";

    private final Logger logger;
    private final boolean logMissingTranslations;
    private final String defaultLocale;
    private final Map<String, Map<String, String>> bundles;

    public TranslationService(Logger logger, LocalizationConfig config) {
        this.logger = Objects.requireNonNull(logger, "logger");
        LocalizationConfig safeConfig = Objects.requireNonNull(config, "config");
        this.logMissingTranslations = safeConfig.logMissingTranslations();
        this.bundles = loadBundles();
        this.defaultLocale = normalizeLocale(safeConfig.defaultLocale());
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public String normalizeLocale(String value) {
        if (value == null || value.isBlank()) {
            return FALLBACK_LOCALE;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.startsWith("ua")) {
            normalized = "uk";
        }
        int separator = normalized.indexOf('_');
        if (separator >= 0) {
            normalized = normalized.substring(0, separator);
        }
        return SUPPORTED_LOCALES.contains(normalized) ? normalized : FALLBACK_LOCALE;
    }

    public String text(String key) {
        return text(defaultLocale, key, Map.of());
    }

    public String text(String key, Map<String, ?> arguments) {
        return text(defaultLocale, key, arguments);
    }

    public String text(String locale, String key) {
        return text(locale, key, Map.of());
    }

    public String text(String locale, String key, Map<String, ?> arguments) {
        Objects.requireNonNull(key, "key");
        String effectiveLocale = normalizeLocale(locale);
        String template = templateFor(effectiveLocale, key);
        return applyArguments(effectiveLocale, template, arguments);
    }

    private Map<String, Map<String, String>> loadBundles() {
        Map<String, Map<String, String>> loaded = new HashMap<>();
        for (String locale : SUPPORTED_LOCALES) {
            loaded.put(locale, loadBundle(locale));
        }
        return Map.copyOf(loaded);
    }

    private Map<String, String> loadBundle(String locale) {
        String resourcePath = "i18n/messages_" + locale + ".properties";
        try (InputStream inputStream = TranslationService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing translation bundle: " + resourcePath);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            Map<String, String> entries = new LinkedHashMap<>();
            for (String key : properties.stringPropertyNames()) {
                entries.put(key, properties.getProperty(key));
            }
            return Map.copyOf(entries);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load translation bundle: " + resourcePath, exception);
        }
    }

    private String templateFor(String locale, String key) {
        String value = bundles.getOrDefault(locale, Map.of()).get(key);
        if (value != null) {
            return value;
        }
        value = bundles.getOrDefault(FALLBACK_LOCALE, Map.of()).get(key);
        if (value != null) {
            if (logMissingTranslations && !FALLBACK_LOCALE.equals(locale)) {
                logger.warning("Missing translation for key '" + key + "' in locale '" + locale + "'. Falling back to '" + FALLBACK_LOCALE + "'.");
            }
            return value;
        }
        if (logMissingTranslations) {
            logger.warning("Missing translation key '" + key + "'.");
        }
        return key;
    }

    private String applyArguments(String locale, String template, Map<String, ?> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = arguments.get(key);
            String replacement = value == null ? matcher.group(0) : formatValue(locale, value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String formatValue(String locale, Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            NumberFormat format = NumberFormat.getIntegerInstance(Locale.forLanguageTag(normalizeLocale(locale)));
            return format.format(value);
        }
        if (value instanceof Number number) {
            NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag(normalizeLocale(locale)));
            format.setGroupingUsed(true);
            format.setMaximumFractionDigits(2);
            format.setMinimumFractionDigits(0);
            return format.format(number.doubleValue());
        }
        return String.valueOf(value);
    }
}
