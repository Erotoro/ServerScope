package com.serverscope.api.lifecycle;

import java.time.Instant;
import java.util.Objects;

public record ComponentHealth(
        ComponentStatus status,
        String details,
        Instant updatedAt
) {
    public ComponentHealth {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static ComponentHealth created() {
        return new ComponentHealth(ComponentStatus.CREATED, "Created", Instant.now());
    }

    public static ComponentHealth running(String details) {
        return new ComponentHealth(ComponentStatus.RUNNING, details, Instant.now());
    }

    public static ComponentHealth stopped(String details) {
        return new ComponentHealth(ComponentStatus.STOPPED, details, Instant.now());
    }

    public static ComponentHealth failed(String details) {
        return new ComponentHealth(ComponentStatus.FAILED, details, Instant.now());
    }
}
