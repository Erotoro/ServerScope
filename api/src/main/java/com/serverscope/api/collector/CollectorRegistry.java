package com.serverscope.api.collector;

import java.util.Collection;
import java.util.Optional;

public interface CollectorRegistry {
    void register(MetricCollector collector);

    Optional<MetricCollector> findById(String collectorId);

    Collection<MetricCollector> all();
}
