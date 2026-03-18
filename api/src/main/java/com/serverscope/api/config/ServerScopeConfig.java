package com.serverscope.api.config;

import java.util.Objects;

public record ServerScopeConfig(
        StorageConfig storage,
        CollectorsConfig collectors,
        WebConfig web,
        AlertingConfig alerts,
        LocalizationConfig localization,
        RetentionConfig retention,
        DebugConfig debug,
        ProfilerConfig profiling,
        SamplingConfig sampling
) {
    public ServerScopeConfig {
        Objects.requireNonNull(storage, "storage");
        Objects.requireNonNull(collectors, "collectors");
        Objects.requireNonNull(web, "web");
        Objects.requireNonNull(alerts, "alerts");
        Objects.requireNonNull(localization, "localization");
        Objects.requireNonNull(retention, "retention");
        Objects.requireNonNull(debug, "debug");
        Objects.requireNonNull(profiling, "profiling");
        Objects.requireNonNull(sampling, "sampling");
    }

    public AlertingConfig alerting() {
        return alerts;
    }

    public ProfilerConfig profiler() {
        return profiling;
    }
}
