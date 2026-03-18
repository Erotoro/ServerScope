package com.serverscope.collectors.server;

import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.serverscope.collectors.CollectorSupport.longSample;

public final class JvmMetricsCollector extends AbstractServerCollector {
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;

    public JvmMetricsCollector(Duration interval) {
        super("jvm-metrics", CollectorSchedule.async(interval));
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = List.copyOf(ManagementFactory.getGarbageCollectorMXBeans());
    }

    @Override
    protected List<MetricSample> collectSamples(Instant timestamp) {
        List<MetricSample> samples = new ArrayList<>(6);
        var heapUsage = memoryBean.getHeapMemoryUsage();
        var nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        samples.add(longSample(MetricType.JVM_HEAP_USED_BYTES, timestamp, heapUsage.getUsed()));
        samples.add(longSample(MetricType.JVM_HEAP_COMMITTED_BYTES, timestamp, heapUsage.getCommitted()));
        samples.add(longSample(MetricType.JVM_HEAP_MAX_BYTES, timestamp, heapUsage.getMax()));
        samples.add(longSample(MetricType.JVM_NON_HEAP_USED_BYTES, timestamp, nonHeapUsage.getUsed()));

        long totalGcCollections = 0L;
        long totalGcCollectionTime = 0L;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long collectionCount = gcBean.getCollectionCount();
            long collectionTime = gcBean.getCollectionTime();
            if (collectionCount >= 0) {
                totalGcCollections += collectionCount;
            }
            if (collectionTime >= 0) {
                totalGcCollectionTime += collectionTime;
            }
        }

        samples.add(longSample(MetricType.JVM_GC_COLLECTION_COUNT, timestamp, totalGcCollections));
        samples.add(longSample(MetricType.JVM_GC_COLLECTION_TIME_MILLIS, timestamp, totalGcCollectionTime));
        return samples;
    }
}
