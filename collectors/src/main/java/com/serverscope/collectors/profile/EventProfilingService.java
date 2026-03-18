package com.serverscope.collectors.profile;

import com.serverscope.api.profile.EventProfileRecord;
import com.serverscope.api.profile.EventProfiler;
import com.serverscope.api.profile.PluginProfileRecord;
import com.serverscope.api.profile.ProfilerSnapshot;
import org.bukkit.event.Event;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

public final class EventProfilingService implements EventProfiler {
    private static final String UNKNOWN_EVENT_CLASS = "unknown";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final int topLimit;
    private final long burstWindowMillis;
    private final long burstMinimumCount;
    private final ConcurrentMap<String, EventAggregate> eventAggregates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PluginAggregate> pluginAggregates = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<Event, EventStartContext>> startContexts = ThreadLocal.withInitial(IdentityHashMap::new);

    public EventProfilingService(JavaPlugin plugin, Logger logger, int topLimit, long burstWindowMillis, long burstMinimumCount) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.topLimit = topLimit;
        this.burstWindowMillis = burstWindowMillis;
        this.burstMinimumCount = burstMinimumCount;
    }

    public void onEventStart(ProfiledEventBinding binding, Event event) {
        Set<String> participatingPlugins = discoverParticipatingPlugins(event);
        startContexts.get().put(event, new EventStartContext(System.nanoTime(), participatingPlugins));
    }

    public void onEventEnd(ProfiledEventBinding binding, Event event) {
        EventStartContext context = startContexts.get().remove(event);
        if (context == null) {
            return;
        }

        long durationNanos = Math.max(0L, System.nanoTime() - context.startedAtNanos());
        recordEvent(binding.id(), binding.eventClass().getName(), durationNanos, context.participatingPlugins());
    }

    @Override
    public ProfilerSnapshot snapshot() {
        List<EventProfileRecord> eventRecords = eventAggregates.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .toList();
        List<PluginProfileRecord> pluginRecords = pluginAggregates.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .toList();

        List<EventProfileRecord> topSlowEvents = eventRecords.stream()
                .sorted(Comparator.comparingLong(EventProfileRecord::totalTimeNanos).reversed())
                .limit(topLimit)
                .toList();
        List<EventProfileRecord> topFrequentEvents = eventRecords.stream()
                .sorted(Comparator.comparingLong(EventProfileRecord::count).reversed())
                .limit(topLimit)
                .toList();
        List<EventProfileRecord> topSuspiciousBursts = eventRecords.stream()
                .filter(record -> record.maxWindowCount() >= burstMinimumCount)
                .sorted(Comparator.comparingDouble(EventProfileRecord::burstScore).reversed()
                        .thenComparingLong(EventProfileRecord::maxWindowCount).reversed())
                .limit(topLimit)
                .toList();
        List<PluginProfileRecord> topPlugins = pluginRecords.stream()
                .sorted(Comparator.comparingLong(PluginProfileRecord::attributedTotalTimeNanos).reversed())
                .limit(topLimit)
                .toList();

        return new ProfilerSnapshot(Instant.now(), topSlowEvents, topFrequentEvents, topSuspiciousBursts, topPlugins);
    }

    private void recordEvent(String eventId, String eventClassName, long durationNanos, Set<String> participatingPlugins) {
        eventAggregates.computeIfAbsent(eventId, ignored -> new EventAggregate(eventClassName, burstWindowMillis))
                .record(durationNanos, participatingPlugins, System.currentTimeMillis());

        if (participatingPlugins.isEmpty()) {
            return;
        }

        long attributedDuration = Math.max(1L, durationNanos / participatingPlugins.size());
        for (String pluginName : participatingPlugins) {
            pluginAggregates.computeIfAbsent(pluginName, ignored -> new PluginAggregate())
                    .record(eventId, attributedDuration);
        }
    }

    private Set<String> discoverParticipatingPlugins(Event event) {
        try {
            RegisteredListener[] listeners = event.getHandlers().getRegisteredListeners();
            LinkedHashSet<String> pluginNames = new LinkedHashSet<>(listeners.length);
            for (RegisteredListener listener : listeners) {
                String pluginName = listener.getPlugin().getName();
                if (!pluginName.equalsIgnoreCase(plugin.getName())) {
                    pluginNames.add(pluginName);
                }
            }
            return pluginNames;
        } catch (RuntimeException exception) {
            logger.fine("Failed to inspect event listeners for " + event.getEventName() + ": " + exception.getMessage());
            return Collections.emptySet();
        }
    }

    private record EventStartContext(long startedAtNanos, Set<String> participatingPlugins) {
    }

    private static final class EventAggregate {
        private final String eventClassName;
        private final long burstWindowMillis;
        private final LongAdder count = new LongAdder();
        private final LongAdder totalTimeNanos = new LongAdder();
        private final LongAccumulator maxTimeNanos = new LongAccumulator(Long::max, 0L);
        private final Set<String> participatingPlugins = ConcurrentHashMap.newKeySet();
        private long firstSeenMillis;
        private long windowStartMillis;
        private long currentWindowCount;
        private long maxWindowCount;

        private EventAggregate(String eventClassName, long burstWindowMillis) {
            this.eventClassName = eventClassName == null || eventClassName.isBlank() ? UNKNOWN_EVENT_CLASS : eventClassName;
            this.burstWindowMillis = burstWindowMillis;
        }

        void record(long durationNanos, Set<String> pluginNames, long nowMillis) {
            count.increment();
            totalTimeNanos.add(durationNanos);
            maxTimeNanos.accumulate(durationNanos);
            participatingPlugins.addAll(pluginNames);
            recordBurst(nowMillis);
        }

        private synchronized void recordBurst(long nowMillis) {
            if (firstSeenMillis == 0L) {
                firstSeenMillis = nowMillis;
            }
            if (windowStartMillis == 0L) {
                windowStartMillis = nowMillis;
            } else if (nowMillis - windowStartMillis >= burstWindowMillis) {
                currentWindowCount = 0L;
                windowStartMillis = nowMillis;
            }

            currentWindowCount += 1L;
            if (currentWindowCount > maxWindowCount) {
                maxWindowCount = currentWindowCount;
            }
        }

        EventProfileRecord snapshot(String eventId) {
            long currentCount = count.sum();
            long currentTotal = totalTimeNanos.sum();
            long currentMax = maxTimeNanos.get();
            long average = currentCount == 0 ? 0L : currentTotal / currentCount;
            long currentMaxWindow;
            long currentFirstSeenMillis;
            synchronized (this) {
                currentMaxWindow = Math.max(maxWindowCount, currentWindowCount);
                currentFirstSeenMillis = firstSeenMillis;
            }
            long elapsedMillis = currentFirstSeenMillis == 0L ? burstWindowMillis : Math.max(burstWindowMillis, System.currentTimeMillis() - currentFirstSeenMillis);
            double windowsObserved = Math.max(1.0d, (double) elapsedMillis / burstWindowMillis);
            double expectedPerWindow = Math.max(1.0d, currentCount / windowsObserved);
            double burstScore = Math.max(1.0d, currentMaxWindow / expectedPerWindow);
            return new EventProfileRecord(
                    eventId,
                    eventClassName,
                    currentCount,
                    currentTotal,
                    currentMax,
                    average,
                    currentMaxWindow,
                    burstScore,
                    burstWindowMillis,
                    participatingPlugins
            );
        }
    }

    private static final class PluginAggregate {
        private final LongAdder eventCount = new LongAdder();
        private final LongAdder attributedTotalTimeNanos = new LongAdder();
        private final LongAccumulator maxAttributedTimeNanos = new LongAccumulator(Long::max, 0L);
        private final Set<String> observedEvents = ConcurrentHashMap.newKeySet();

        void record(String eventId, long attributedDuration) {
            eventCount.increment();
            attributedTotalTimeNanos.add(attributedDuration);
            maxAttributedTimeNanos.accumulate(attributedDuration);
            observedEvents.add(eventId);
        }

        PluginProfileRecord snapshot(String pluginName) {
            long currentCount = eventCount.sum();
            long currentTotal = attributedTotalTimeNanos.sum();
            long currentMax = maxAttributedTimeNanos.get();
            long average = currentCount == 0 ? 0L : currentTotal / currentCount;
            return new PluginProfileRecord(pluginName, currentCount, currentTotal, currentMax, average, observedEvents);
        }
    }
}
