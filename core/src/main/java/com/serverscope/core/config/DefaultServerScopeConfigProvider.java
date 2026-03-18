package com.serverscope.core.config;

import com.serverscope.api.config.ServerScopeConfig;
import com.serverscope.api.config.ServerScopeConfigProvider;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultServerScopeConfigProvider implements ServerScopeConfigProvider {
    private final AtomicReference<ServerScopeConfig> currentConfig;

    public DefaultServerScopeConfigProvider(ServerScopeConfig initialConfig) {
        this.currentConfig = new AtomicReference<>(Objects.requireNonNull(initialConfig, "initialConfig"));
    }

    @Override
    public ServerScopeConfig current() {
        return currentConfig.get();
    }

    public void replace(ServerScopeConfig newConfig) {
        currentConfig.set(Objects.requireNonNull(newConfig, "newConfig"));
    }
}
