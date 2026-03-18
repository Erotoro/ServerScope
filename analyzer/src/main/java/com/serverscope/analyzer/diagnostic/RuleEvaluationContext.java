package com.serverscope.analyzer.diagnostic;

import com.serverscope.api.config.AlertThresholdsConfig;
import com.serverscope.api.profile.ProfilerSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RuleEvaluationContext(
        Instant now,
        AlertThresholdsConfig thresholds,
        Optional<ServerHealthSample> currentServerHealth,
        List<ServerHealthSample> recentHealthSamples,
        Map<String, Long> worldLoadedChunks,
        List<ChunkDiagnosticSample> hotChunks,
        ProfilerSnapshot profilerSnapshot
) {
}
