package com.serverscope.collectors.server;

import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import org.bukkit.Server;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.serverscope.collectors.CollectorSupport.longSample;

public final class TotalEntitiesCollector extends AbstractServerCollector {
    private final Server server;

    public TotalEntitiesCollector(Server server, Duration interval) {
        super("server-total-entities", CollectorSchedule.platformSafe(interval));
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected List<MetricSample> collectSamples(Instant timestamp) {
        return ServerRuntimeMetricAccess.totalEntities(server)
                .stream()
                .mapToObj(value -> longSample(MetricType.ENTITY_COUNT, timestamp, value))
                .toList();
    }
}
