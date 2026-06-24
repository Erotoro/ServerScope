package com.serverscope.collectors.runtime;

import com.serverscope.api.collector.CollectorContext;
import com.serverscope.api.collector.CollectorExecutionMode;
import com.serverscope.api.collector.CollectorRegistry;
import com.serverscope.api.collector.MetricCollector;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.core.concurrent.NamedThreadFactory;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CollectorExecutionService {
    private final Logger logger;
    private final CollectorRegistry collectorRegistry;
    private final ScheduledExecutorService asyncExecutor;
    private final PlatformCollectorScheduler platformScheduler;
    private final CollectorContext collectorContext;
    private final Map<String, MetricBatch> latestBatches;
    private final List<CollectorTaskHandle> handles;

    public CollectorExecutionService(JavaPlugin plugin, Logger logger, CollectorRegistry collectorRegistry) {
        Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.collectorRegistry = Objects.requireNonNull(collectorRegistry, "collectorRegistry");
        ThreadFactory asyncThreadFactory = NamedThreadFactory.daemonNumbered("serverscope-collector-async");
        this.asyncExecutor = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = asyncThreadFactory.newThread(runnable);
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    this.logger.log(Level.SEVERE, "Unhandled collector async failure", throwable));
            return thread;
        });
        this.platformScheduler = new PlatformCollectorScheduler(plugin);
        this.collectorContext = new CollectorContext(Clock.systemUTC());
        this.latestBatches = new ConcurrentHashMap<>();
        this.handles = new ArrayList<>();
    }

    public void start() {
        for (MetricCollector collector : collectorRegistry.all()) {
            handles.add(scheduleCollector(collector));
        }
    }

    public void stop() {
        for (CollectorTaskHandle handle : handles) {
            try {
                handle.cancel();
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, "Failed to cancel collector task", exception);
            }
        }
        handles.clear();
        asyncExecutor.shutdownNow();
    }

    public Map<String, MetricBatch> latestBatches() {
        return Map.copyOf(latestBatches);
    }

    private CollectorTaskHandle scheduleCollector(MetricCollector collector) {
        long initialDelayMillis = collector.schedule().initialDelay().toMillis();
        long intervalMillis = collector.schedule().interval().toMillis();

        Runnable task = () -> executeCollector(collector);
        if (collector.schedule().executionMode() == CollectorExecutionMode.ASYNC_BACKGROUND) {
            var future = asyncExecutor.scheduleAtFixedRate(task, initialDelayMillis, intervalMillis, TimeUnit.MILLISECONDS);
            logger.info("Registered async collector " + collector.collectorId() + " every " + intervalMillis + "ms");
            return () -> future.cancel(false);
        }

        logger.info("Registered platform-safe collector " + collector.collectorId() + " every " + intervalMillis + "ms");
        return platformScheduler.scheduleRepeating(task, initialDelayMillis, intervalMillis);
    }

    private void executeCollector(MetricCollector collector) {
        try {
            MetricBatch batch = collector.collect(collectorContext);
            if (!batch.isEmpty()) {
                latestBatches.put(collector.collectorId(), batch);
            }
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Collector " + collector.collectorId() + " failed", exception);
        }
    }
}
