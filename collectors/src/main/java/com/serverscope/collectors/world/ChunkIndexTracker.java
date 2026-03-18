package com.serverscope.collectors.world;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public final class ChunkIndexTracker implements Listener {
    private final ConcurrentMap<String, Set<ChunkCoordinate>> loadedChunksByWorld = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> loadedChunkCountByWorld = new ConcurrentHashMap<>();

    public void seedWorld(World world) {
        Objects.requireNonNull(world, "world");
        Set<ChunkCoordinate> chunks = loadedChunksByWorld.computeIfAbsent(world.getName(), ignored -> ConcurrentHashMap.newKeySet());
        for (Chunk chunk : world.getLoadedChunks()) {
            chunks.add(new ChunkCoordinate(world.getName(), chunk.getX(), chunk.getZ()));
        }
        LongAdder counter = loadedChunkCountByWorld.computeIfAbsent(world.getName(), ignored -> new LongAdder());
        counter.reset();
        counter.add(chunks.size());
    }

    public long loadedChunkCount(String worldName) {
        LongAdder counter = loadedChunkCountByWorld.get(worldName);
        return counter == null ? 0L : counter.sum();
    }

    public List<ChunkCoordinate> sampleLoadedChunks(int maxCount) {
        List<ChunkCoordinate> snapshot = new ArrayList<>();
        for (Set<ChunkCoordinate> coordinates : loadedChunksByWorld.values()) {
            snapshot.addAll(coordinates);
        }

        if (snapshot.size() <= maxCount) {
            return snapshot;
        }

        List<ChunkCoordinate> result = new ArrayList<>(maxCount);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < maxCount; i++) {
            int index = random.nextInt(snapshot.size());
            result.add(snapshot.remove(index));
        }
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        String worldName = event.getWorld().getName();
        ChunkCoordinate coordinate = new ChunkCoordinate(worldName, event.getChunk().getX(), event.getChunk().getZ());
        Set<ChunkCoordinate> chunks = loadedChunksByWorld.computeIfAbsent(worldName, ignored -> ConcurrentHashMap.newKeySet());
        if (chunks.add(coordinate)) {
            loadedChunkCountByWorld.computeIfAbsent(worldName, ignored -> new LongAdder()).increment();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        String worldName = event.getWorld().getName();
        ChunkCoordinate coordinate = new ChunkCoordinate(worldName, event.getChunk().getX(), event.getChunk().getZ());
        Set<ChunkCoordinate> chunks = loadedChunksByWorld.get(worldName);
        if (chunks != null && chunks.remove(coordinate)) {
            loadedChunkCountByWorld.computeIfAbsent(worldName, ignored -> new LongAdder()).decrement();
        }
    }
}
