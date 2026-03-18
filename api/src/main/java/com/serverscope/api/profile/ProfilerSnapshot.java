package com.serverscope.api.profile;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProfilerSnapshot(
        Instant capturedAt,
        List<EventProfileRecord> topSlowEvents,
        List<EventProfileRecord> topFrequentEvents,
        List<EventProfileRecord> topSuspiciousBursts,
        List<PluginProfileRecord> topPlugins
) {
    public ProfilerSnapshot {
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(topSlowEvents, "topSlowEvents");
        Objects.requireNonNull(topFrequentEvents, "topFrequentEvents");
        Objects.requireNonNull(topSuspiciousBursts, "topSuspiciousBursts");
        Objects.requireNonNull(topPlugins, "topPlugins");
        topSlowEvents = List.copyOf(topSlowEvents);
        topFrequentEvents = List.copyOf(topFrequentEvents);
        topSuspiciousBursts = List.copyOf(topSuspiciousBursts);
        topPlugins = List.copyOf(topPlugins);
    }
}
