package com.serverscope.core.lifecycle;

import com.serverscope.api.lifecycle.ComponentHealth;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.lifecycle.ManagedComponent;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractManagedComponent implements ManagedComponent {
    private final String name;
    private final AtomicReference<ComponentHealth> health;

    protected AbstractManagedComponent(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.health = new AtomicReference<>(ComponentHealth.created());
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final ComponentHealth health() {
        return health.get();
    }

    protected final void updateHealth(ComponentStatus status, String details) {
        health.set(new ComponentHealth(status, details, Instant.now()));
    }
}
