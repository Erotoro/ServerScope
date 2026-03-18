package com.serverscope.analyzer.diagnostic;

import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MetricSnapshotExtractor {
    public Optional<ServerHealthSample> extractServerHealth(Map<String, MetricBatch> batches) {
        Optional<MetricSample> tps = latestServerMetric(batches, MetricType.SERVER_TPS);
        Optional<MetricSample> mspt = latestServerMetric(batches, MetricType.SERVER_MSPT);
        Optional<MetricSample> heapUsed = latestServerMetric(batches, MetricType.JVM_HEAP_USED_BYTES);
        Optional<MetricSample> heapMax = latestServerMetric(batches, MetricType.JVM_HEAP_MAX_BYTES);
        Optional<MetricSample> loadedChunks = latestServerMetric(batches, MetricType.LOADED_CHUNKS);
        Optional<MetricSample> totalEntities = latestServerMetric(batches, MetricType.ENTITY_COUNT);
        Optional<MetricSample> players = latestServerMetric(batches, MetricType.PLAYERS_ONLINE);

        if (tps.isEmpty() || mspt.isEmpty() || heapUsed.isEmpty() || heapMax.isEmpty() || players.isEmpty()) {
            return Optional.empty();
        }

        Instant timestamp = List.of(
                        tps.get().timestamp(),
                        mspt.get().timestamp(),
                        heapUsed.get().timestamp(),
                        heapMax.get().timestamp(),
                        players.get().timestamp(),
                        loadedChunks.map(MetricSample::timestamp).orElse(tps.get().timestamp()),
                        totalEntities.map(MetricSample::timestamp).orElse(tps.get().timestamp())
                ).stream()
                .max(Comparator.naturalOrder())
                .orElse(tps.get().timestamp());

        return Optional.of(new ServerHealthSample(
                timestamp,
                tps.get().numericValue(),
                mspt.get().numericValue(),
                heapUsed.get().longValue(),
                Math.max(1L, heapMax.get().longValue()),
                loadedChunks.map(MetricSample::longValue).orElse(0L),
                totalEntities.map(MetricSample::longValue).orElse(0L),
                (int) players.get().longValue()
        ));
    }

    public Map<String, Long> extractWorldLoadedChunks(Map<String, MetricBatch> batches) {
        Map<String, MetricSample> latest = new HashMap<>();
        for (MetricBatch batch : batches.values()) {
            for (MetricSample sample : batch.samples()) {
                if (sample.type() != MetricType.LOADED_CHUNKS) {
                    continue;
                }
                String world = sample.labels().get("world");
                if (world == null || world.isBlank()) {
                    continue;
                }
                latest.merge(world, sample, MetricSnapshotExtractor::latestByTimestamp);
            }
        }

        Map<String, Long> result = new HashMap<>();
        latest.forEach((world, sample) -> result.put(world, sample.longValue()));
        return Map.copyOf(result);
    }

    public List<ChunkDiagnosticSample> extractHotChunks(Map<String, MetricBatch> batches, int limit) {
        Map<String, MetricSample> latestEntity = new HashMap<>();
        Map<String, MetricSample> latestBlockEntities = new HashMap<>();

        for (MetricBatch batch : batches.values()) {
            for (MetricSample sample : batch.samples()) {
                String key = chunkKey(sample);
                if (key == null) {
                    continue;
                }
                if (sample.type() == MetricType.ENTITY_COUNT) {
                    latestEntity.merge(key, sample, MetricSnapshotExtractor::latestByTimestamp);
                } else if (sample.type() == MetricType.BLOCK_ENTITY_COUNT) {
                    latestBlockEntities.merge(key, sample, MetricSnapshotExtractor::latestByTimestamp);
                }
            }
        }

        List<ChunkDiagnosticSample> result = new ArrayList<>();
        for (String key : latestEntity.keySet()) {
            MetricSample entity = latestEntity.get(key);
            MetricSample block = latestBlockEntities.get(key);
            if (entity == null) {
                continue;
            }
            Integer chunkX = parseChunkCoordinate(entity.labels().get("chunk_x"));
            Integer chunkZ = parseChunkCoordinate(entity.labels().get("chunk_z"));
            if (chunkX == null || chunkZ == null) {
                continue;
            }
            long blockEntityCount = block == null ? 0L : block.longValue();
            result.add(new ChunkDiagnosticSample(
                    entity.timestamp(),
                    entity.labels().get("world"),
                    chunkX,
                    chunkZ,
                    entity.longValue(),
                    blockEntityCount,
                    entity.longValue() * 10L + blockEntityCount * 25L
            ));
        }

        return result.stream()
                .sorted(Comparator.comparingLong(ChunkDiagnosticSample::hotspotScore).reversed())
                .limit(limit)
                .toList();
    }

    private Optional<MetricSample> latestServerMetric(Map<String, MetricBatch> batches, MetricType type) {
        return batches.values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type() == type)
                .filter(sample -> sample.labels().isEmpty())
                .max(Comparator.comparing(MetricSample::timestamp));
    }

    private static MetricSample latestByTimestamp(MetricSample left, MetricSample right) {
        return right.timestamp().isAfter(left.timestamp()) ? right : left;
    }

    private static String chunkKey(MetricSample sample) {
        String world = sample.labels().get("world");
        String chunkX = sample.labels().get("chunk_x");
        String chunkZ = sample.labels().get("chunk_z");
        if (world == null || chunkX == null || chunkZ == null) {
            return null;
        }
        return world + ":" + chunkX + ":" + chunkZ;
    }

    private static Integer parseChunkCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
