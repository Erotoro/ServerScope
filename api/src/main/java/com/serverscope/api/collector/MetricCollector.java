package com.serverscope.api.collector;

import com.serverscope.api.metric.MetricBatch;

public interface MetricCollector {
    String collectorId();

    CollectorSchedule schedule();

    MetricBatch collect(CollectorContext context);
}
