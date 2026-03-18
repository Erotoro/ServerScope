package com.serverscope.collectors;

import com.serverscope.api.config.CollectorsConfig;
import com.serverscope.api.config.ProfilerConfig;
import com.serverscope.api.collector.CollectorRegistry;
import com.serverscope.api.lifecycle.ComponentStatus;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.collectors.runtime.CollectorExecutionService;
import com.serverscope.collectors.runtime.PlatformCollectorScheduler;
import com.serverscope.collectors.runtime.RegionTaskScheduler;
import com.serverscope.collectors.profile.EventProfilingRegistrar;
import com.serverscope.collectors.profile.EventProfilingService;
import com.serverscope.collectors.profile.ProfiledEventCatalog;
import com.serverscope.collectors.server.JvmMetricsCollector;
import com.serverscope.collectors.server.LoadedChunksCollector;
import com.serverscope.collectors.server.OnlinePlayersCollector;
import com.serverscope.collectors.server.TotalEntitiesCollector;
import com.serverscope.collectors.server.TpsMsptCollector;
import com.serverscope.collectors.world.ChunkIndexTracker;
import com.serverscope.collectors.world.ChunkSnapshotCollector;
import com.serverscope.collectors.world.WorldSnapshotCollector;
import com.serverscope.core.collector.DefaultCollectorRegistry;
import com.serverscope.core.lifecycle.AbstractManagedComponent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class CollectorsModule extends AbstractManagedComponent {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final CollectorsConfig config;
    private final ProfilerConfig profilerConfig;
    private final CollectorRegistry collectorRegistry;
    private final CollectorExecutionService executionService;
    private final ChunkIndexTracker chunkIndexTracker;
    private final PlatformCollectorScheduler platformScheduler;
    private final EventProfilingService eventProfilingService;
    private final EventProfilingRegistrar eventProfilingRegistrar;

    public CollectorsModule(JavaPlugin plugin, Logger logger, CollectorsConfig config, ProfilerConfig profilerConfig) {
        super("collectors");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.profilerConfig = Objects.requireNonNull(profilerConfig, "profilerConfig");
        this.collectorRegistry = new DefaultCollectorRegistry();
        this.chunkIndexTracker = new ChunkIndexTracker();
        this.platformScheduler = new PlatformCollectorScheduler(plugin);
        this.executionService = new CollectorExecutionService(plugin, logger, collectorRegistry);
        this.eventProfilingService = new EventProfilingService(
                plugin,
                logger,
                profilerConfig.topLimit(),
                profilerConfig.burstWindowMillis(),
                profilerConfig.burstMinimumCount()
        );
        this.eventProfilingRegistrar = new EventProfilingRegistrar(plugin, logger, eventProfilingService);
        registerCollectors();
    }

    public CollectorRegistry collectorRegistry() {
        return collectorRegistry;
    }

    public Collection<String> registeredCollectorIds() {
        return collectorRegistry.all().stream()
                .map(metricCollector -> metricCollector.collectorId())
                .sorted()
                .toList();
    }

    public Map<String, MetricBatch> latestBatches() {
        return executionService.latestBatches();
    }

    public ProfilerSnapshot profilerSnapshot() {
        return eventProfilingService.snapshot();
    }

    @Override
    public void start() {
        if (!config.enabled()) {
            logger.info("Collectors module is disabled by configuration");
            updateHealth(ComponentStatus.RUNNING, "Collectors disabled by configuration");
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(chunkIndexTracker, plugin);
        platformScheduler.execute(this::seedChunkIndex);
        executionService.start();
        registerProfilerIfEnabled();
        logger.info("Collectors module initialized with " + collectorRegistry.all().size() + " registered collectors");
        updateHealth(ComponentStatus.RUNNING, "Collectors registry is active");
    }

    @Override
    public void stop() {
        executionService.stop();
        eventProfilingRegistrar.unregisterAll();
        HandlerList.unregisterAll(chunkIndexTracker);
        logger.info("Collectors module stopped");
        updateHealth(ComponentStatus.STOPPED, "Collectors registry is stopped");
    }

    private void registerCollectors() {
        registerServerCollectors();
        registerWorldCollectors();
    }

    private void registerServerCollectors() {
        var serverConfig = config.server();
        collectorRegistry.register(new TpsMsptCollector(plugin.getServer(), java.time.Duration.ofMillis(serverConfig.tpsMsptIntervalMillis())));
        collectorRegistry.register(new OnlinePlayersCollector(plugin.getServer(), java.time.Duration.ofMillis(serverConfig.playersIntervalMillis())));
        collectorRegistry.register(new JvmMetricsCollector(java.time.Duration.ofMillis(serverConfig.jvmIntervalMillis())));
        collectorRegistry.register(new LoadedChunksCollector(plugin.getServer(), java.time.Duration.ofMillis(serverConfig.loadedChunksIntervalMillis())));
        collectorRegistry.register(new TotalEntitiesCollector(plugin.getServer(), java.time.Duration.ofMillis(serverConfig.totalEntitiesIntervalMillis())));
    }

    private void registerWorldCollectors() {
        var worldConfig = config.world();
        collectorRegistry.register(new WorldSnapshotCollector(
                plugin.getServer(),
                chunkIndexTracker,
                java.time.Duration.ofMillis(worldConfig.worldSnapshotIntervalMillis())
        ));
        collectorRegistry.register(new ChunkSnapshotCollector(
                plugin,
                chunkIndexTracker,
                new RegionTaskScheduler(),
                java.time.Duration.ofMillis(worldConfig.chunkSamplingIntervalMillis()),
                worldConfig.maxChunksPerRun()
        ));
    }

    private void seedChunkIndex() {
        if (isFoliaRuntime()) {
            logger.info("Skipping eager chunk index seed on Folia; index will be built from chunk load/unload events");
            return;
        }
        for (var world : plugin.getServer().getWorlds()) {
            chunkIndexTracker.seedWorld(world);
        }
    }

    private boolean isFoliaRuntime() {
        String serverName = plugin.getServer().getName();
        if (serverName != null && serverName.toLowerCase(java.util.Locale.ROOT).contains("folia")) {
            return true;
        }
        String version = plugin.getServer().getVersion();
        return version != null && version.toLowerCase(java.util.Locale.ROOT).contains("folia");
    }

    private void registerProfilerIfEnabled() {
        if (!profilerConfig.enabled()) {
            return;
        }

        for (String eventId : profilerConfig.eventIds()) {
            ProfiledEventCatalog.find(eventId).ifPresentOrElse(
                    eventProfilingRegistrar::register,
                    () -> logger.warning("Skipping unsupported profiled event id: " + eventId)
            );
        }
    }
}
