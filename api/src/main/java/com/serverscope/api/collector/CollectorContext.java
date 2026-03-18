package com.serverscope.api.collector;

import java.time.Clock;
import java.util.Objects;

public record CollectorContext(Clock clock) {
    public CollectorContext {
        Objects.requireNonNull(clock, "clock");
    }
}
