package com.serverscope.api.config;

import java.util.Objects;

public record WebConfig(
        boolean enabled,
        String host,
        int port,
        String authToken,
        boolean corsEnabled,
        String corsAllowedOrigin,
        boolean reverseProxyEnabled,
        int maxRequestsPerWindow,
        long rateLimitWindowMillis,
        int maxRequestUriLength,
        int maxResponseBytes
) {
    public WebConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(authToken, "authToken");
        Objects.requireNonNull(corsAllowedOrigin, "corsAllowedOrigin");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (enabled && authToken.isBlank()) {
            throw new IllegalArgumentException("authToken must not be blank when web is enabled");
        }
        if (maxRequestsPerWindow <= 0) {
            throw new IllegalArgumentException("maxRequestsPerWindow must be positive");
        }
        if (rateLimitWindowMillis <= 0L) {
            throw new IllegalArgumentException("rateLimitWindowMillis must be positive");
        }
        if (maxRequestUriLength <= 0) {
            throw new IllegalArgumentException("maxRequestUriLength must be positive");
        }
        if (maxResponseBytes <= 0) {
            throw new IllegalArgumentException("maxResponseBytes must be positive");
        }
    }
}
