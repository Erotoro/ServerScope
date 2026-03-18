package com.serverscope.api.collector;

import java.time.Duration;
import java.util.Objects;

public record CollectorSchedule(
        Duration initialDelay,
        Duration interval,
        CollectorExecutionMode executionMode
) {
    public CollectorSchedule {
        Objects.requireNonNull(initialDelay, "initialDelay");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(executionMode, "executionMode");
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("initialDelay must not be negative");
        }
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
    }

    public static CollectorSchedule async(Duration interval) {
        return new CollectorSchedule(Duration.ZERO, interval, CollectorExecutionMode.ASYNC_BACKGROUND);
    }

    public static CollectorSchedule platformSafe(Duration interval) {
        return new CollectorSchedule(Duration.ZERO, interval, CollectorExecutionMode.PLATFORM_SAFE);
    }
}
