package com.serverscope.api.metric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MetricLabels(Map<String, String> values) {
    public static final MetricLabels EMPTY = new MetricLabels(Map.of());

    public MetricLabels {
        Objects.requireNonNull(values, "values");
        values = Map.copyOf(values);
    }

    public static MetricLabels of(String key, String value) {
        return new MetricLabels(Map.of(key, value));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String get(String key) {
        return values.get(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public static final class Builder {
        private final Map<String, String> values = new LinkedHashMap<>();

        public Builder add(String key, String value) {
            values.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        public MetricLabels build() {
            return values.isEmpty() ? EMPTY : new MetricLabels(values);
        }
    }
}
