package com.serverscope.collectors.world;

import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.collector.MetricCollector;
import com.serverscope.api.collector.CollectorContext;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricLabels;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import com.serverscope.collectors.server.ServerRuntimeMetricAccess;
import org.bukkit.Server;
import org.bukkit.World;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.serverscope.collectors.CollectorSupport.longSample;

public final class WorldSnapshotCollector implements MetricCollector {
    private final Server server;
    private final ChunkIndexTracker chunkIndexTracker;
    private final CollectorSchedule schedule;

    public WorldSnapshotCollector(Server server, ChunkIndexTracker chunkIndexTracker, Duration interval) {
        this.server = Objects.requireNonNull(server, "server");
        this.chunkIndexTracker = Objects.requireNonNull(chunkIndexTracker, "chunkIndexTracker");
        this.schedule = CollectorSchedule.platformSafe(interval);
    }

    @Override
    public String collectorId() {
        return "world-snapshots";
    }

    @Override
    public CollectorSchedule schedule() {
        return schedule;
    }

    @Override
    public MetricBatch collect(CollectorContext context) {
        Instant timestamp = Instant.now(context.clock());
        List<MetricSample> samples = new ArrayList<>();
        for (World world : server.getWorlds()) {
            long loadedChunkCount = ServerRuntimeMetricAccess.loadedChunksForWorld(world)
                    .orElseGet(() -> chunkIndexTracker.loadedChunkCount(world.getName()));
            samples.add(longSample(
                    MetricType.LOADED_CHUNKS,
                    timestamp,
                    loadedChunkCount,
                    MetricLabels.builder().add("world", world.getName()).build()
            ));
        }
        return new MetricBatch(collectorId(), timestamp, samples);
    }
}
