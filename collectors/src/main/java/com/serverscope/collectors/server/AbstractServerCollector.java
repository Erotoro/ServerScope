package com.serverscope.collectors.server;

import com.serverscope.api.collector.CollectorContext;
import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.collector.MetricCollector;
import com.serverscope.api.metric.MetricBatch;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public abstract class AbstractServerCollector implements MetricCollector {
    private final String collectorId;
    private final CollectorSchedule schedule;

    protected AbstractServerCollector(String collectorId, CollectorSchedule schedule) {
        this.collectorId = Objects.requireNonNull(collectorId, "collectorId");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
    }

    @Override
    public final String collectorId() {
        return collectorId;
    }

    @Override
    public final CollectorSchedule schedule() {
        return schedule;
    }

    @Override
    public final MetricBatch collect(CollectorContext context) {
        Instant timestamp = Instant.now(context.clock());
        return new MetricBatch(collectorId, timestamp, collectSamples(timestamp));
    }

    protected abstract List<com.serverscope.api.metric.MetricSample> collectSamples(Instant timestamp);
}
