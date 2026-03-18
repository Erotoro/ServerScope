package com.serverscope.web.api;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.profile.EventProfileRecord;
import com.serverscope.api.profile.PluginProfileRecord;

import java.time.Instant;
import java.util.Map;
import java.util.List;

public record OverviewResponse(
        Instant capturedAt,
        Map<String, MetricValueResponse> serverMetrics,
        List<AlertRecord> activeAlerts,
        List<DiagnosticFinding> activeFindings,
        List<EventProfileRecord> topSlowEvents,
        List<EventProfileRecord> topFrequentEvents,
        List<EventProfileRecord> topSuspiciousBursts,
        List<PluginProfileRecord> topPlugins
) {
}
