package com.serverscope.api.storage;

import java.util.List;

public interface MetricSampleRepository {
    List<MetricSample> findLatestMetricSamples(int limit);
}
