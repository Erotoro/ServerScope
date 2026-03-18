package com.serverscope.bootstrap.config;

public final class ServerScopeConfigKeys {
    private ServerScopeConfigKeys() {
    }

    public static final class Storage {
        public static final String ENABLED = "storage.enabled";
        public static final String SQLITE_FILE = "storage.sqlite-file";
        public static final String QUEUE_CAPACITY = "storage.queue-capacity";
        public static final String MAX_BATCH_SIZE = "storage.max-batch-size";
        public static final String FLUSH_INTERVAL_MILLIS = "storage.flush-interval-millis";

        private Storage() {
        }
    }

    public static final class Collectors {
        public static final String ENABLED = "collectors.enabled";
        public static final String SERVER_TPS_MSPT_INTERVAL = "collectors.server.tps-mspt-interval-millis";
        public static final String SERVER_PLAYERS_INTERVAL = "collectors.server.players-interval-millis";
        public static final String SERVER_JVM_INTERVAL = "collectors.server.jvm-interval-millis";
        public static final String SERVER_LOADED_CHUNKS_INTERVAL = "collectors.server.loaded-chunks-interval-millis";
        public static final String SERVER_TOTAL_ENTITIES_INTERVAL = "collectors.server.total-entities-interval-millis";
        public static final String WORLD_SNAPSHOT_INTERVAL = "collectors.world.snapshot-interval-millis";
        public static final String WORLD_CHUNK_SAMPLING_INTERVAL = "collectors.world.chunk-sampling-interval-millis";
        public static final String WORLD_MAX_CHUNKS_PER_RUN = "collectors.world.max-chunks-per-run";

        private Collectors() {
        }
    }

    public static final class Web {
        public static final String ENABLED = "web.enabled";
        public static final String HOST = "web.host";
        public static final String PORT = "web.port";
        public static final String AUTH_TOKEN = "web.auth-token";
        public static final String CORS_ENABLED = "web.cors-enabled";
        public static final String CORS_ALLOWED_ORIGIN = "web.cors-allowed-origin";
        public static final String REVERSE_PROXY_ENABLED = "web.reverse-proxy-enabled";
        public static final String MAX_REQUESTS_PER_WINDOW = "web.max-requests-per-window";
        public static final String RATE_LIMIT_WINDOW_MILLIS = "web.rate-limit-window-millis";
        public static final String MAX_REQUEST_URI_LENGTH = "web.max-request-uri-length";
        public static final String MAX_RESPONSE_BYTES = "web.max-response-bytes";

        private Web() {
        }
    }

    public static final class Alerts {
        public static final String ENABLED = "alerts.enabled";
        public static final String LEGACY_ENABLED = "alerting.enabled";
        public static final String EVALUATION_INTERVAL_MILLIS = "alerts.evaluation-interval-millis";
        public static final String LEGACY_EVALUATION_INTERVAL_MILLIS = "alerting.evaluation-interval-millis";
        public static final String COOLDOWN_MILLIS = "alerts.cooldown-millis";
        public static final String LEGACY_COOLDOWN_MILLIS = "alerting.cooldown-millis";
        public static final String RATE_LIMIT_MILLIS = "alerts.rate-limit-millis";
        public static final String LEGACY_RATE_LIMIT_MILLIS = "alerting.rate-limit-millis";
        public static final String THRESHOLD_LOW_TPS = "alerts.thresholds.low-tps";
        public static final String LEGACY_THRESHOLD_LOW_TPS = "alerting.thresholds.low-tps";
        public static final String THRESHOLD_HIGH_MSPT = "alerts.thresholds.high-mspt";
        public static final String LEGACY_THRESHOLD_HIGH_MSPT = "alerting.thresholds.high-mspt";
        public static final String THRESHOLD_HIGH_ENTITY_COUNT = "alerts.thresholds.high-entity-count";
        public static final String LEGACY_THRESHOLD_HIGH_ENTITY_COUNT = "alerting.thresholds.high-entity-count";
        public static final String THRESHOLD_HIGH_CHUNK_ENTITY_COUNT = "alerts.thresholds.high-chunk-entity-count";
        public static final String LEGACY_THRESHOLD_HIGH_CHUNK_ENTITY_COUNT = "alerting.thresholds.high-chunk-entity-count";
        public static final String THRESHOLD_HIGH_CHUNK_BLOCK_ENTITY_COUNT = "alerts.thresholds.high-chunk-block-entity-count";
        public static final String LEGACY_THRESHOLD_HIGH_CHUNK_BLOCK_ENTITY_COUNT = "alerting.thresholds.high-chunk-block-entity-count";
        public static final String THRESHOLD_HIGH_EVENT_AVERAGE_MILLIS = "alerts.thresholds.high-event-average-millis";
        public static final String LEGACY_THRESHOLD_HIGH_EVENT_AVERAGE_MILLIS = "alerting.thresholds.high-event-average-millis";
        public static final String CHANNEL_CONSOLE_ENABLED = "alerts.channels.console-enabled";
        public static final String LEGACY_CHANNEL_CONSOLE_ENABLED = "alerting.channels.console-enabled";
        public static final String CHANNEL_INGAME_ENABLED = "alerts.channels.in-game-enabled";
        public static final String LEGACY_CHANNEL_INGAME_ENABLED = "alerting.channels.in-game-enabled";
        public static final String CHANNEL_WEBHOOK_ENABLED = "alerts.channels.webhook-enabled";
        public static final String LEGACY_CHANNEL_WEBHOOK_ENABLED = "alerting.channels.webhook-enabled";
        public static final String CHANNEL_WEBHOOK_URL = "alerts.channels.webhook-url";
        public static final String LEGACY_CHANNEL_WEBHOOK_URL = "alerting.channels.webhook-url";
        public static final String CHANNEL_ADMIN_PERMISSION = "alerts.channels.admin-permission";
        public static final String LEGACY_CHANNEL_ADMIN_PERMISSION = "alerting.channels.admin-permission";

        private Alerts() {
        }
    }

    public static final class Localization {
        public static final String DEFAULT_LOCALE = "localization.default-locale";
        public static final String LOG_MISSING_TRANSLATIONS = "localization.log-missing-translations";

        private Localization() {
        }
    }

    public static final class Retention {
        public static final String ENABLED = "retention.enabled";
        public static final String METRIC_SAMPLES_DAYS = "retention.metric-samples-days";
        public static final String ALERTS_DAYS = "retention.alerts-days";
        public static final String PROFILING_DAYS = "retention.profiling-days";
        public static final String CHUNK_SNAPSHOTS_DAYS = "retention.chunk-snapshots-days";
        public static final String LEGACY_STORAGE_RETENTION_DAYS = "storage.retention-days";

        private Retention() {
        }
    }

    public static final class Debug {
        public static final String ENABLED = "debug.enabled";
        public static final String VERBOSE_LOGGING = "debug.verbose-logging";
        public static final String LOG_CONFIG_RELOADS = "debug.log-config-reloads";

        private Debug() {
        }
    }

    public static final class Profiling {
        public static final String ENABLED = "profiling.enabled";
        public static final String LEGACY_ENABLED = "profiler.enabled";
        public static final String TOP_LIMIT = "profiling.top-limit";
        public static final String LEGACY_TOP_LIMIT = "profiler.top-limit";
        public static final String BURST_WINDOW_MILLIS = "profiling.burst-window-millis";
        public static final String LEGACY_BURST_WINDOW_MILLIS = "profiler.burst-window-millis";
        public static final String BURST_MINIMUM_COUNT = "profiling.burst-minimum-count";
        public static final String LEGACY_BURST_MINIMUM_COUNT = "profiler.burst-minimum-count";
        public static final String EVENTS = "profiling.events";
        public static final String LEGACY_EVENTS = "profiler.events";

        private Profiling() {
        }
    }

    public static final class Sampling {
        public static final String SKIP_OVERLAPPING_CHUNK_SAMPLING = "sampling.skip-overlapping-chunk-sampling";
        public static final String DEFAULT_MAX_CHUNKS_PER_RUN = "sampling.default-max-chunks-per-run";
        public static final String DEFAULT_INTERVAL_MILLIS = "sampling.default-interval-millis";

        private Sampling() {
        }
    }
}
