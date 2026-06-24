package com.serverscope.api.storage;

import java.time.Instant;
import java.util.List;

public interface MetricSampleRepository {
    List<MetricSample> findLatestMetricSamples(int limit);

    /**
     * Returns aggregate server-health samples recorded at or after {@code since}, ordered from
     * oldest to newest so callers can render them directly as a time series. When more than
     * {@code limit} samples match, the most recent {@code limit} are returned.
     *
     * @param since lower (inclusive) bound on sample time
     * @param limit maximum number of samples to return; must be positive
     * @return chronologically ascending samples, never {@code null}
     */
    List<MetricSample> findMetricSamplesSince(Instant since, int limit);
}
