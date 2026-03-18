package com.serverscope.collectors;

import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricLabels;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;

import java.time.Instant;
import java.util.List;

public final class CollectorSupport {
    private CollectorSupport() {
    }

    public static MetricSample longSample(MetricType type, Instant timestamp, long value) {
        return MetricSample.ofLong(type, timestamp, value, MetricLabels.EMPTY);
    }

    public static MetricSample longSample(MetricType type, Instant timestamp, long value, MetricLabels labels) {
        return MetricSample.ofLong(type, timestamp, value, labels);
    }

    public static MetricSample doubleSample(MetricType type, Instant timestamp, double value) {
        return MetricSample.ofDouble(type, timestamp, value, MetricLabels.EMPTY);
    }

    public static MetricSample doubleSample(MetricType type, Instant timestamp, double value, MetricLabels labels) {
        return MetricSample.ofDouble(type, timestamp, value, labels);
    }

    public static MetricBatch batch(String collectorId, Instant collectedAt, List<MetricSample> samples) {
        return new MetricBatch(collectorId, collectedAt, samples);
    }
}
