package com.serverscope.bootstrap.config;

import java.util.List;

public final class ServerScopeConfigDefaults {
    private ServerScopeConfigDefaults() {
    }

    public static final boolean STORAGE_ENABLED = true;
    public static final String STORAGE_SQLITE_FILE = "serverscope-mvp.db";
    public static final int STORAGE_QUEUE_CAPACITY = 8192;
    public static final int STORAGE_MAX_BATCH_SIZE = 512;
    public static final long STORAGE_FLUSH_INTERVAL_MILLIS = 1500L;

    public static final boolean COLLECTORS_ENABLED = true;
    public static final long SERVER_TPS_MSPT_INTERVAL_MILLIS = 1000L;
    public static final long SERVER_PLAYERS_INTERVAL_MILLIS = 2000L;
    public static final long SERVER_JVM_INTERVAL_MILLIS = 5000L;
    public static final long SERVER_LOADED_CHUNKS_INTERVAL_MILLIS = 10000L;
    public static final long SERVER_TOTAL_ENTITIES_INTERVAL_MILLIS = 10000L;
    public static final long WORLD_SNAPSHOT_INTERVAL_MILLIS = 10000L;
    public static final long WORLD_CHUNK_SAMPLING_INTERVAL_MILLIS = 15000L;
    public static final int WORLD_MAX_CHUNKS_PER_RUN = 24;

    public static final boolean WEB_ENABLED = true;
    public static final String WEB_HOST = "127.0.0.1";
    public static final int WEB_PORT = 8080;
    public static final String WEB_AUTH_TOKEN = "auto-generated-per-server";
    public static final boolean WEB_CORS_ENABLED = false;
    public static final String WEB_CORS_ALLOWED_ORIGIN = "http://127.0.0.1";
    public static final boolean WEB_REVERSE_PROXY_ENABLED = false;
    public static final int WEB_MAX_REQUESTS_PER_WINDOW = 120;
    public static final long WEB_RATE_LIMIT_WINDOW_MILLIS = 10000L;
    public static final int WEB_MAX_REQUEST_URI_LENGTH = 2048;
    public static final int WEB_MAX_RESPONSE_BYTES = 2_097_152;

    public static final boolean ALERTS_ENABLED = true;
    public static final long ALERTS_EVALUATION_INTERVAL_MILLIS = 10000L;
    public static final long ALERTS_COOLDOWN_MILLIS = 120000L;
    public static final long ALERTS_RATE_LIMIT_MILLIS = 15000L;
    public static final double ALERTS_LOW_TPS = 17.0d;
    public static final double ALERTS_HIGH_MSPT = 65.0d;
    public static final long ALERTS_HIGH_ENTITY_COUNT = 3500L;
    public static final long ALERTS_HIGH_CHUNK_ENTITY_COUNT = 120L;
    public static final long ALERTS_HIGH_CHUNK_BLOCK_ENTITY_COUNT = 60L;
    public static final double ALERTS_HIGH_EVENT_AVERAGE_MILLIS = 20.0d;
    public static final boolean ALERTS_CHANNEL_CONSOLE_ENABLED = true;
    public static final boolean ALERTS_CHANNEL_INGAME_ENABLED = true;
    public static final boolean ALERTS_CHANNEL_WEBHOOK_ENABLED = false;
    public static final String ALERTS_CHANNEL_WEBHOOK_URL = "https://example.invalid/serverscope-webhook";
    public static final String ALERTS_CHANNEL_ADMIN_PERMISSION = "serverscope.alerts";

    public static final String LOCALIZATION_DEFAULT_LOCALE = "en";
    public static final boolean LOCALIZATION_LOG_MISSING_TRANSLATIONS = true;

    public static final boolean RETENTION_ENABLED = true;
    public static final int RETENTION_METRIC_SAMPLES_DAYS = 21;
    public static final int RETENTION_ALERTS_DAYS = 21;
    public static final int RETENTION_PROFILING_DAYS = 14;
    public static final int RETENTION_CHUNK_SNAPSHOTS_DAYS = 14;

    public static final boolean DEBUG_ENABLED = false;
    public static final boolean DEBUG_VERBOSE_LOGGING = false;
    public static final boolean DEBUG_LOG_CONFIG_RELOADS = true;

    public static final boolean PROFILING_ENABLED = true;
    public static final int PROFILING_TOP_LIMIT = 8;
    public static final long PROFILING_BURST_WINDOW_MILLIS = 15000L;
    public static final long PROFILING_BURST_MINIMUM_COUNT = 40L;
    public static final List<String> PROFILING_EVENTS = List.of(
            "player_interact",
            "block_break",
            "block_place",
            "entity_damage",
            "inventory_click",
            "creature_spawn"
    );

    public static final boolean SAMPLING_SKIP_OVERLAPPING_CHUNK_SAMPLING = true;
    public static final int SAMPLING_DEFAULT_MAX_CHUNKS_PER_RUN = 24;
    public static final long SAMPLING_DEFAULT_INTERVAL_MILLIS = 7000L;
}
