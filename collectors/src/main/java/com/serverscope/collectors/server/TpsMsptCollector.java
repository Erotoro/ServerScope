package com.serverscope.collectors.server;

import com.serverscope.api.collector.CollectorSchedule;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.metric.MetricType;
import org.bukkit.Server;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.serverscope.collectors.CollectorSupport.doubleSample;

public final class TpsMsptCollector extends AbstractServerCollector {
    private final Server server;

    public TpsMsptCollector(Server server, Duration interval) {
        super("server-tps-mspt", CollectorSchedule.platformSafe(interval));
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    protected List<MetricSample> collectSamples(Instant timestamp) {
        List<MetricSample> samples = new ArrayList<>(2);
        ServerRuntimeMetricAccess.primaryTps(server)
                .ifPresent(tps -> samples.add(doubleSample(MetricType.SERVER_TPS, timestamp, tps)));
        ServerRuntimeMetricAccess.averageTickTimeMillis(server)
                .ifPresent(mspt -> samples.add(doubleSample(MetricType.SERVER_MSPT, timestamp, mspt)));
        return samples;
    }
}
