package com.serverscope.core.collector;

import com.serverscope.api.collector.CollectorRegistry;
import com.serverscope.api.collector.MetricCollector;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DefaultCollectorRegistry implements CollectorRegistry {
    private final ConcurrentMap<String, MetricCollector> collectors = new ConcurrentHashMap<>();

    @Override
    public void register(MetricCollector collector) {
        MetricCollector existing = collectors.putIfAbsent(
                Objects.requireNonNull(collector, "collector").collectorId(),
                collector
        );
        if (existing != null) {
            throw new IllegalArgumentException("Collector already registered: " + collector.collectorId());
        }
    }

    @Override
    public Optional<MetricCollector> findById(String collectorId) {
        return Optional.ofNullable(collectors.get(Objects.requireNonNull(collectorId, "collectorId")));
    }

    @Override
    public Collection<MetricCollector> all() {
        return List.copyOf(collectors.values());
    }
}
