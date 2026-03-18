package com.serverscope.api.config;

public record DebugConfig(
        boolean enabled,
        boolean verboseLogging,
        boolean logConfigReloads
) {
}
