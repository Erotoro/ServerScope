package com.serverscope.collectors.runtime;

import com.serverscope.api.metric.MetricSample;
import com.serverscope.collectors.world.ChunkCoordinate;
import com.serverscope.collectors.world.ChunkSnapshotCollector;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class RegionTaskScheduler {
    public CompletableFuture<List<MetricSample>> sampleChunk(Plugin plugin, ChunkCoordinate coordinate, Instant timestamp) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(coordinate, "coordinate");
        Objects.requireNonNull(timestamp, "timestamp");

        CompletableFuture<List<MetricSample>> future = new CompletableFuture<>();
        World world = Bukkit.getWorld(coordinate.worldName());
        if (world == null) {
            future.complete(List.of());
            return future;
        }

        Runnable task = () -> {
            try {
                future.complete(ChunkSnapshotCollector.sampleChunk(world, coordinate.chunkX(), coordinate.chunkZ(), timestamp));
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
            }
        };

        if (scheduleRegionTask(plugin, world, coordinate.chunkX(), coordinate.chunkZ(), task)) {
            return future;
        }

        Bukkit.getScheduler().runTask(plugin, task);
        return future;
    }

    private boolean scheduleRegionTask(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task) {
        try {
            Method getter = plugin.getServer().getClass().getMethod("getRegionScheduler");
            Object regionScheduler = getter.invoke(plugin.getServer());
            Method execute = regionScheduler.getClass().getMethod(
                    "execute",
                    Plugin.class,
                    World.class,
                    int.class,
                    int.class,
                    Runnable.class
            );
            execute.invoke(regionScheduler, plugin, world, chunkX, chunkZ, task);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method getter = plugin.getServer().getClass().getMethod("getRegionScheduler");
            Object regionScheduler = getter.invoke(plugin.getServer());
            Method run = regionScheduler.getClass().getMethod(
                    "run",
                    Plugin.class,
                    World.class,
                    int.class,
                    int.class,
                    Consumer.class
            );
            run.invoke(regionScheduler, plugin, world, chunkX, chunkZ, (Consumer<Object>) ignored -> task.run());
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
