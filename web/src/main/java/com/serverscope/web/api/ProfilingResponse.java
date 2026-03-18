package com.serverscope.web.api;

import com.serverscope.api.profile.EventProfileRecord;
import com.serverscope.api.profile.PluginProfileRecord;

import java.time.Instant;
import java.util.List;

public record ProfilingResponse(
        Instant capturedAt,
        List<PluginProfileRecord> topPlugins,
        List<EventProfileRecord> topSlowEvents,
        List<EventProfileRecord> topFrequentEvents,
        List<EventProfileRecord> topSuspiciousBursts
) {
}
