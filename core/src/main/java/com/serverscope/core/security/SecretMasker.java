package com.serverscope.core.security;

import java.util.Objects;
import java.util.regex.Pattern;

public final class SecretMasker {
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile("(?i)(Authorization\\s*[:=]\\s*Bearer\\s+)([^\\s,;]+)");
    private static final Pattern AUTH_TOKEN = Pattern.compile("(?i)(auth-token\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern X_SERVER_SCOPE_TOKEN = Pattern.compile("(?i)(X-ServerScope-Token\\s*[:=]\\s*)([^\\s,;]+)");
    private static final Pattern WEBHOOK_URL = Pattern.compile("(?i)(webhook-url\\s*[:=]\\s*)([^\\s,;]+)");

    private SecretMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = value;
        masked = AUTHORIZATION_BEARER.matcher(masked).replaceAll("$1***");
        masked = AUTH_TOKEN.matcher(masked).replaceAll("$1***");
        masked = X_SERVER_SCOPE_TOKEN.matcher(masked).replaceAll("$1***");
        masked = WEBHOOK_URL.matcher(masked).replaceAll("$1***");
        return masked;
    }

    public static String mask(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable");
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + mask(message);
    }
}
