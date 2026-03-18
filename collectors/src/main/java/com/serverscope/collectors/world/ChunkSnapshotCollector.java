package com.serverscope.collectors.world;

import com.serverscope.api.collector.CollectorContext;
import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.collector.MetricCollector;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricLabels;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import com.serverscope.collectors.runtime.RegionTaskScheduler;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.serverscope.collectors.CollectorSupport.longSample;

public final class ChunkSnapshotCollector implements MetricCollector {
    private final JavaPlugin plugin;
    private final ChunkIndexTracker chunkIndexTracker;
    private final RegionTaskScheduler regionTaskScheduler;
    private final CollectorSchedule schedule;
    private final int maxChunksPerRun;
    private final AtomicBoolean samplingInProgress = new AtomicBoolean(false);
    private final AtomicReference<MetricBatch> latestCompletedBatch = new AtomicReference<>();

    public ChunkSnapshotCollector(
            JavaPlugin plugin,
            ChunkIndexTracker chunkIndexTracker,
            RegionTaskScheduler regionTaskScheduler,
            Duration interval,
            int maxChunksPerRun
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.chunkIndexTracker = Objects.requireNonNull(chunkIndexTracker, "chunkIndexTracker");
        this.regionTaskScheduler = Objects.requireNonNull(regionTaskScheduler, "regionTaskScheduler");
        this.schedule = CollectorSchedule.async(interval);
        this.maxChunksPerRun = maxChunksPerRun;
    }

    @Override
    public String collectorId() {
        return "chunk-snapshots";
    }

    @Override
    public CollectorSchedule schedule() {
        return schedule;
    }

    @Override
    public MetricBatch collect(CollectorContext context) {
        Instant timestamp = Instant.now(context.clock());
        triggerSampling(timestamp);
        MetricBatch readyBatch = latestCompletedBatch.getAndSet(null);
        return readyBatch == null ? new MetricBatch(collectorId(), timestamp, List.of()) : readyBatch;
    }

    private void triggerSampling(Instant timestamp) {
        if (!samplingInProgress.compareAndSet(false, true)) {
            return;
        }

        List<ChunkCoordinate> sampleTargets = chunkIndexTracker.sampleLoadedChunks(maxChunksPerRun);
        if (sampleTargets.isEmpty()) {
            latestCompletedBatch.set(new MetricBatch(collectorId(), timestamp, List.of()));
            samplingInProgress.set(false);
            return;
        }

        List<CompletableFuture<List<MetricSample>>> futures = new ArrayList<>(sampleTargets.size());
        for (ChunkCoordinate coordinate : sampleTargets) {
            futures.add(regionTaskScheduler.sampleChunk(plugin, coordinate, timestamp));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, throwable) -> {
                    try {
                        List<MetricSample> samples = new ArrayList<>();
                        for (CompletableFuture<List<MetricSample>> future : futures) {
                            samples.addAll(future.handle((result, error) -> error == null ? result : List.<MetricSample>of()).join());
                        }
                        latestCompletedBatch.set(new MetricBatch(collectorId(), timestamp, samples));
                    } finally {
                        samplingInProgress.set(false);
                    }
                });
    }

    public static List<MetricSample> sampleChunk(World world, int chunkX, int chunkZ, Instant timestamp) {
        Optional<Chunk> chunk = resolveLoadedChunk(world, chunkX, chunkZ);
        if (chunk.isEmpty()) {
            return List.of();
        }

        MetricLabels labels = MetricLabels.builder()
                .add("world", world.getName())
                .add("chunk_x", Integer.toString(chunkX))
                .add("chunk_z", Integer.toString(chunkZ))
                .build();

        List<MetricSample> samples = new ArrayList<>(2);
        samples.add(longSample(MetricType.ENTITY_COUNT, timestamp, chunk.get().getEntities().length, labels));

        int blockEntityCount = chunk.get().getTileEntities().length;
        samples.add(longSample(MetricType.BLOCK_ENTITY_COUNT, timestamp, blockEntityCount, labels));
        return samples;
    }

    private static Optional<Chunk> resolveLoadedChunk(World world, int chunkX, int chunkZ) {
        try {
            var method = world.getClass().getMethod("getChunkAtIfLoaded", int.class, int.class);
            Object value = method.invoke(world, chunkX, chunkZ);
            if (value instanceof Chunk chunk) {
                return Optional.of(chunk);
            }
            return Optional.empty();
        } catch (ReflectiveOperationException ignored) {
        }

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return Optional.empty();
        }
        return Optional.of(world.getChunkAt(chunkX, chunkZ));
    }
}
