package com.serverscope.analyzer.alert;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.AlertingConfig;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AlertDispatcher {
    private final AlertingConfig config;
    private final List<AlertNotifier> notifiers;
    private final ConcurrentMap<String, DispatchState> dispatchStates = new ConcurrentHashMap<>();

    public AlertDispatcher(AlertingConfig config, List<AlertNotifier> notifiers) {
        this.config = Objects.requireNonNull(config, "config");
        this.notifiers = List.copyOf(Objects.requireNonNull(notifiers, "notifiers"));
    }

    public void dispatch(AlertRecord alertRecord) {
        long now = System.currentTimeMillis();
        String fingerprint = fingerprint(alertRecord);
        DispatchState state = dispatchStates.computeIfAbsent(alertRecord.dedupeKey(), ignored -> new DispatchState());
        if (!state.shouldDispatch(now, fingerprint, config.rateLimitMillis())) {
            return;
        }

        for (AlertNotifier notifier : notifiers) {
            notifier.notify(alertRecord);
        }
    }

    private String fingerprint(AlertRecord alertRecord) {
        return alertRecord.status()
                + "|"
                + alertRecord.severity()
                + "|"
                + alertRecord.code()
                + "|"
                + alertRecord.message();
    }

    private static final class DispatchState {
        private long lastDispatchAtMillis;
        private String lastFingerprint = "";

        private synchronized boolean shouldDispatch(long now, String fingerprint, long rateLimitMillis) {
            boolean changed = !fingerprint.equals(lastFingerprint);
            if (!changed && now - lastDispatchAtMillis < rateLimitMillis) {
                return false;
            }
            lastDispatchAtMillis = now;
            lastFingerprint = fingerprint;
            return true;
        }
    }
}
