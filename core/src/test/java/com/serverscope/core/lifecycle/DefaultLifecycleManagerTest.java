package com.serverscope.core.lifecycle;

import com.serverscope.api.lifecycle.ComponentHealth;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.lifecycle.ManagedComponent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultLifecycleManagerTest {
    @Test
    void startAllAllowsExplicitlyDegradedComponentToFinishStartup() {
        StubComponent strictRunning = StubComponent.running("storage");
        StubComponent degraded = StubComponent.failedButTolerated("web");

        DefaultLifecycleManager manager = new DefaultLifecycleManager(
                Logger.getLogger("test"),
                List.of(strictRunning, degraded)
        );

        assertDoesNotThrow(manager::startAll);
        assertTrue(degraded.started.get());
    }

    @Test
    void startAllStillRejectsStrictFailedComponent() {
        StubComponent strictRunning = StubComponent.running("storage");
        StubComponent strictFailed = StubComponent.failed("analyzer");

        DefaultLifecycleManager manager = new DefaultLifecycleManager(
                Logger.getLogger("test"),
                List.of(strictRunning, strictFailed)
        );

        assertThrows(IllegalStateException.class, manager::startAll);
        assertTrue(strictRunning.stopped.get());
    }

    private static final class StubComponent implements ManagedComponent {
        private final String name;
        private final boolean tolerateDegradedStartup;
        private final ComponentStatus finalStatus;
        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean stopped = new AtomicBoolean();
        private ComponentHealth health = ComponentHealth.created();

        private StubComponent(String name, boolean tolerateDegradedStartup, ComponentStatus finalStatus) {
            this.name = name;
            this.tolerateDegradedStartup = tolerateDegradedStartup;
            this.finalStatus = finalStatus;
        }

        static StubComponent running(String name) {
            return new StubComponent(name, false, ComponentStatus.RUNNING);
        }

        static StubComponent failed(String name) {
            return new StubComponent(name, false, ComponentStatus.FAILED);
        }

        static StubComponent failedButTolerated(String name) {
            return new StubComponent(name, true, ComponentStatus.FAILED);
        }

        @Override
        public void start() {
            started.set(true);
            health = new ComponentHealth(finalStatus, finalStatus.name(), Instant.now());
        }

        @Override
        public void stop() {
            stopped.set(true);
            health = ComponentHealth.stopped("stopped");
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public ComponentHealth health() {
            return health;
        }

        @Override
        public boolean toleratesDegradedStartup() {
            return tolerateDegradedStartup;
        }
    }
}
