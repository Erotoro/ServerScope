package com.serverscope.core.lifecycle;

import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.lifecycle.ManagedComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DefaultLifecycleManager implements LifecycleManager {
    private final Logger logger;
    private final List<ManagedComponent> components;

    public DefaultLifecycleManager(Logger logger, List<ManagedComponent> components) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.components = List.copyOf(Objects.requireNonNull(components, "components"));
    }

    @Override
    public void startAll() {
        List<ManagedComponent> started = new ArrayList<>();
        for (ManagedComponent component : components) {
            try {
                logger.info(() -> "Starting component " + component.name());
                component.start();
                if (component.health().status() != ComponentStatus.RUNNING) {
                    throw new IllegalStateException("Component " + component.name() + " did not reach RUNNING state");
                }
                started.add(component);
            } catch (RuntimeException exception) {
                stopStartedComponents(started);
                throw exception;
            }
        }
    }

    @Override
    public void stopAll() {
        List<ManagedComponent> reverse = new ArrayList<>(components);
        Collections.reverse(reverse);
        for (ManagedComponent component : reverse) {
            try {
                logger.info(() -> "Stopping component " + component.name());
                component.stop();
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Failed to stop component " + component.name(), exception);
            }
        }
    }

    @Override
    public List<ManagedComponent> components() {
        return components;
    }

    private void stopStartedComponents(List<ManagedComponent> started) {
        List<ManagedComponent> reverse = new ArrayList<>(started);
        Collections.reverse(reverse);
        for (ManagedComponent component : reverse) {
            try {
                logger.warning(() -> "Rolling back component startup for " + component.name());
                component.stop();
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Failed to roll back component " + component.name(), exception);
            }
        }
    }
}
