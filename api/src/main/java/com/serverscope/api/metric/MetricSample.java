package com.serverscope.api.metric;

import java.time.Instant;
import java.util.Objects;

public record MetricSample(
        MetricType type,
        Instant timestamp,
        double numericValue,
        MetricLabels labels
) {
    public MetricSample {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(labels, "labels");
    }

    public static MetricSample ofDouble(MetricType type, Instant timestamp, double value, MetricLabels labels) {
        if (type.valueType() != MetricValueType.DOUBLE_GAUGE) {
            throw new IllegalArgumentException("Metric type " + type + " does not accept double values");
        }
        return new MetricSample(type, timestamp, value, labels);
    }

    public static MetricSample ofLong(MetricType type, Instant timestamp, long value, MetricLabels labels) {
        if (type.valueType() == MetricValueType.DOUBLE_GAUGE) {
            throw new IllegalArgumentException("Metric type " + type + " does not accept long values");
        }
        return new MetricSample(type, timestamp, value, labels);
    }

    public long longValue() {
        return Math.round(numericValue);
    }
}
