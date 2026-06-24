package com.serverscope.analyzer.diagnostic;

import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.diagnostic.DiagnosticReference;
import com.serverscope.api.diagnostic.FindingConfidence;
import com.serverscope.api.diagnostic.FindingSeverity;
import com.serverscope.api.profile.EventProfileRecord;
import com.serverscope.core.i18n.TranslationService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DiagnosticRules {
    private DiagnosticRules() {
    }

    public static List<DiagnosticRule> defaults(TranslationService translations) {
        return List.of(
                context -> lowTpsHighMspt(context, translations),
                context -> entitySpike(context, translations),
                context -> memoryPressure(context, translations),
                context -> suspiciousChunkHotspot(context, translations),
                context -> frequentEventBurst(context, translations),
                context -> loadedChunkSpike(context, translations),
                context -> unstableServerTrend(context, translations)
        );
    }

    private static Optional<DiagnosticFinding> lowTpsHighMspt(RuleEvaluationContext context, TranslationService translations) {
        return context.currentServerHealth().flatMap(current -> {
            boolean lowTps = current.tps() < context.thresholds().lowTps();
            boolean highMspt = current.mspt() > context.thresholds().highMspt();
            if (!lowTps && !highMspt) {
                return Optional.empty();
            }

            FindingSeverity severity = lowTps && highMspt ? FindingSeverity.CRITICAL : FindingSeverity.WARN;
            String cause = correlationHint(context, current, translations);
            return Optional.of(new DiagnosticFinding(
                    "server.health.tick-degradation",
                    context.now(),
                    severity,
                    translations.text("finding.server.health.tick_degradation.title"),
                    translations.text("finding.server.health.tick_degradation.description", args(
                            "tps", current.tps(),
                            "mspt", current.mspt()
                    )),
                    cause,
                    translations.text("finding.server.health.tick_degradation.action"),
                    severity == FindingSeverity.CRITICAL ? FindingConfidence.HIGH : FindingConfidence.MEDIUM,
                    null
            ));
        });
    }

    private static Optional<DiagnosticFinding> entitySpike(RuleEvaluationContext context, TranslationService translations) {
        return context.currentServerHealth().flatMap(current -> {
            List<ServerHealthSample> baselineSamples = historicalBaselineSamples(context.recentHealthSamples());
            double baseline = average(baselineSamples.stream().mapToLong(ServerHealthSample::totalEntities).boxed().toList());
            long delta = Math.round(current.totalEntities() - baseline);
            if (current.totalEntities() < context.thresholds().highEntityCount() && !(baseline > 0 && current.totalEntities() > baseline * 1.25 && delta >= 250)) {
                return Optional.empty();
            }

            return Optional.of(new DiagnosticFinding(
                    "server.entities.spike",
                    context.now(),
                    current.totalEntities() >= context.thresholds().highEntityCount() ? FindingSeverity.WARN : FindingSeverity.INFO,
                    translations.text("finding.server.entities_spike.title"),
                    translations.text("finding.server.entities_spike.description", args(
                            "entities", current.totalEntities(),
                            "baseline", baseline
                    )),
                    translations.text("finding.server.entities_spike.cause"),
                    translations.text("finding.server.entities_spike.action"),
                    baseline > 0 ? FindingConfidence.MEDIUM : FindingConfidence.LOW,
                    null
            ));
        });
    }

    private static Optional<DiagnosticFinding> memoryPressure(RuleEvaluationContext context, TranslationService translations) {
        return context.currentServerHealth().flatMap(current -> {
            double heapRatio = current.heapUsedBytes() / (double) Math.max(1L, current.heapMaxBytes());
            if (heapRatio < 0.85d) {
                return Optional.empty();
            }

            FindingSeverity severity = heapRatio >= 0.93d ? FindingSeverity.CRITICAL : FindingSeverity.WARN;
            return Optional.of(new DiagnosticFinding(
                    "server.memory.pressure",
                    context.now(),
                    severity,
                    translations.text("finding.server.memory_pressure.title"),
                    translations.text("finding.server.memory_pressure.description", args(
                            "heapPercent", heapRatio * 100.0d
                    )),
                    translations.text("finding.server.memory_pressure.cause"),
                    translations.text("finding.server.memory_pressure.action"),
                    heapRatio >= 0.93d ? FindingConfidence.HIGH : FindingConfidence.MEDIUM,
                    null
            ));
        });
    }

    private static Optional<DiagnosticFinding> suspiciousChunkHotspot(RuleEvaluationContext context, TranslationService translations) {
        return context.hotChunks().stream()
                .max(Comparator.comparingLong(ChunkDiagnosticSample::hotspotScore))
                .filter(chunk -> chunk.entityCount() >= context.thresholds().highChunkEntityCount()
                        || chunk.blockEntityCount() >= context.thresholds().highChunkBlockEntityCount())
                .map(chunk -> new DiagnosticFinding(
                        "chunk.hotspot.%s.%d.%d".formatted(chunk.worldName(), chunk.chunkX(), chunk.chunkZ()),
                        context.now(),
                        FindingSeverity.WARN,
                        translations.text("finding.chunk.hotspot.title"),
                        translations.text("finding.chunk.hotspot.description", args(
                                "chunkX", chunk.chunkX(),
                                "chunkZ", chunk.chunkZ(),
                                "world", chunk.worldName(),
                                "entities", chunk.entityCount(),
                                "blockEntities", chunk.blockEntityCount()
                        )),
                        translations.text("finding.chunk.hotspot.cause"),
                        translations.text("finding.chunk.hotspot.action"),
                        FindingConfidence.HIGH,
                        new DiagnosticReference(chunk.worldName(), chunk.chunkX(), chunk.chunkZ(), null)
                ));
    }

    private static Optional<DiagnosticFinding> frequentEventBurst(RuleEvaluationContext context, TranslationService translations) {
        if (context.profilerSnapshot() == null) {
            return Optional.empty();
        }

        return context.profilerSnapshot().topSuspiciousBursts().stream()
                .filter(record -> record.maxWindowCount() >= 1)
                .max(Comparator.comparingDouble(EventProfileRecord::burstScore))
                .map(record -> new DiagnosticFinding(
                        "event.burst.%s".formatted(record.eventId()),
                        context.now(),
                        record.burstScore() >= 3.0d ? FindingSeverity.WARN : FindingSeverity.INFO,
                        translations.text("finding.event.burst.title"),
                        translations.text("finding.event.burst.description", args(
                                "eventId", record.eventId(),
                                "maxWindowCount", record.maxWindowCount(),
                                "windowSeconds", Math.max(1L, record.burstWindowMillis() / 1000L)
                        )),
                        translations.text("finding.event.burst.cause"),
                        translations.text("finding.event.burst.action"),
                        record.burstScore() >= 3.0d ? FindingConfidence.HIGH : FindingConfidence.MEDIUM,
                        pluginReference(record.participatingPlugins())
                ));
    }

    private static Optional<DiagnosticFinding> loadedChunkSpike(RuleEvaluationContext context, TranslationService translations) {
        return context.currentServerHealth().flatMap(current -> {
            List<ServerHealthSample> baselineSamples = historicalBaselineSamples(context.recentHealthSamples());
            double baseline = average(baselineSamples.stream().mapToLong(ServerHealthSample::loadedChunks).boxed().toList());
            long delta = Math.round(current.loadedChunks() - baseline);
            if (!(baseline > 0 && current.loadedChunks() > baseline * 1.35d && delta >= 160)) {
                return Optional.empty();
            }

            String busiestWorld = context.worldLoadedChunks().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            return Optional.of(new DiagnosticFinding(
                    "server.chunks.spike",
                    context.now(),
                    FindingSeverity.WARN,
                    translations.text("finding.server.chunks_spike.title"),
                    translations.text("finding.server.chunks_spike.description", args(
                            "loadedChunks", current.loadedChunks(),
                            "baseline", baseline
                    )),
                    translations.text("finding.server.chunks_spike.cause"),
                    translations.text("finding.server.chunks_spike.action"),
                    FindingConfidence.MEDIUM,
                    busiestWorld == null ? null : new DiagnosticReference(busiestWorld, null, null, null)
            ));
        });
    }

    private static Optional<DiagnosticFinding> unstableServerTrend(RuleEvaluationContext context, TranslationService translations) {
        List<ServerHealthSample> samples = context.recentHealthSamples();
        if (samples.size() < 8) {
            return Optional.empty();
        }

        int split = samples.size() / 2;
        List<ServerHealthSample> older = samples.subList(0, split);
        List<ServerHealthSample> newer = samples.subList(split, samples.size());
        double olderMspt = averageDouble(older.stream().mapToDouble(ServerHealthSample::mspt).boxed().toList());
        double newerMspt = averageDouble(newer.stream().mapToDouble(ServerHealthSample::mspt).boxed().toList());
        double olderTps = averageDouble(older.stream().mapToDouble(ServerHealthSample::tps).boxed().toList());
        double newerTps = averageDouble(newer.stream().mapToDouble(ServerHealthSample::tps).boxed().toList());

        boolean meaningfulMsptRegression = newerMspt > olderMspt * 1.35d && newerMspt - olderMspt >= 3.0d;
        boolean meaningfulTpsRegression = newerTps < olderTps - 1.25d;
        if (!(meaningfulMsptRegression || meaningfulTpsRegression)) {
            return Optional.empty();
        }

        return Optional.of(new DiagnosticFinding(
                "server.health.unstable-trend",
                context.now(),
                FindingSeverity.WARN,
                translations.text("finding.server.unstable_trend.title"),
                translations.text("finding.server.unstable_trend.description", args(
                        "olderMspt", olderMspt,
                        "olderTps", olderTps,
                        "newerMspt", newerMspt,
                        "newerTps", newerTps
                )),
                translations.text("finding.server.unstable_trend.cause"),
                translations.text("finding.server.unstable_trend.action"),
                FindingConfidence.MEDIUM,
                null
        ));
    }

    private static DiagnosticReference pluginReference(java.util.Set<String> plugins) {
        if (plugins.size() == 1) {
            return new DiagnosticReference(null, null, null, plugins.iterator().next());
        }
        return null;
    }

    private static String correlationHint(RuleEvaluationContext context, ServerHealthSample current, TranslationService translations) {
        if (!context.hotChunks().isEmpty() && context.hotChunks().get(0).hotspotScore() > 0L) {
            ChunkDiagnosticSample chunk = context.hotChunks().get(0);
            return translations.text("finding.server.health.tick_degradation.cause.chunk", args(
                    "chunkX", chunk.chunkX(),
                    "chunkZ", chunk.chunkZ(),
                    "world", chunk.worldName(),
                    "entities", chunk.entityCount(),
                    "blockEntities", chunk.blockEntityCount()
            ));
        }
        if (context.profilerSnapshot() != null && !context.profilerSnapshot().topSuspiciousBursts().isEmpty()) {
            EventProfileRecord burst = context.profilerSnapshot().topSuspiciousBursts().get(0);
            return translations.text("finding.server.health.tick_degradation.cause.event_burst", args(
                    "eventId", burst.eventId(),
                    "maxWindowCount", burst.maxWindowCount()
            ));
        }
        if (current.totalEntities() >= context.thresholds().highEntityCount()) {
            return translations.text("finding.server.health.tick_degradation.cause.entity_pressure");
        }
        return translations.text("finding.server.health.tick_degradation.cause.generic");
    }

    private static Map<String, Object> args(Object... entries) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            arguments.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return arguments;
    }

    private static double average(List<Long> values) {
        if (values.isEmpty()) {
            return 0.0d;
        }
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0d);
    }

    private static double averageDouble(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0d;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    }

    private static List<ServerHealthSample> historicalBaselineSamples(List<ServerHealthSample> samples) {
        if (samples.size() <= 1) {
            return samples;
        }
        return samples.subList(0, samples.size() - 1);
    }
}
