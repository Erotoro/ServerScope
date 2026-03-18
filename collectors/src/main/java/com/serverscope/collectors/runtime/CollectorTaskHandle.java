package com.serverscope.collectors.runtime;

@FunctionalInterface
public interface CollectorTaskHandle {
    void cancel();
}
