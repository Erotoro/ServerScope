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

public final class LoadedChunksCollector extends AbstractServerCollector {
    private final Server server;

    public LoadedChunksCollector(Server server, Duration interval) {
        super("server-loaded-chunks", CollectorSchedule.platformSafe(interval));
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected List<MetricSample> collectSamples(Instant timestamp) {
        return ServerRuntimeMetricAccess.totalLoadedChunks(server)
                .stream()
                .mapToObj(value -> longSample(MetricType.LOADED_CHUNKS, timestamp, value))
                .toList();
    }
}
