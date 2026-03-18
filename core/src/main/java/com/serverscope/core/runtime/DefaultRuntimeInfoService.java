package com.serverscope.core.runtime;

import com.serverscope.api.service.RuntimeInfoService;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultRuntimeInfoService implements RuntimeInfoService {
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public boolean isStarted() {
        return started.get();
    }

    public void markStarted() {
        started.set(true);
    }

    public void markStopped() {
        started.set(false);
    }
}
