package com.serverscope.bootstrap.config;

import com.serverscope.api.config.AlertChannelsConfig;
import com.serverscope.api.config.AlertThresholdsConfig;
import com.serverscope.api.config.AlertingConfig;
import com.serverscope.api.config.CollectorsConfig;
import com.serverscope.api.config.DebugConfig;
import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.ProfilerConfig;
import com.serverscope.api.config.RetentionConfig;
import com.serverscope.api.config.SamplingConfig;
import com.serverscope.api.config.ServerCollectorsConfig;
import com.serverscope.api.config.ServerScopeConfig;
import com.serverscope.api.config.StorageConfig;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.config.WorldCollectorsConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public final class BootstrapConfigLoader {
    private static final String LEGACY_WEB_AUTH_TOKEN_PLACEHOLDER = "change-me-serverscope-token";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JavaPlugin plugin;
    private final Logger logger;

    public BootstrapConfigLoader(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
    }

    public void ensureDefaultConfigExists() {
        plugin.saveDefaultConfig();
        ensureServerSpecificSecrets();
    }

    public ServerScopeConfig load() {
        ensureDefaultConfigExists();
        return loadFromDisk();
    }

    public ServerScopeConfig loadFromDisk() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);
        return load(yamlConfiguration);
    }

    public String regenerateWebAuthToken() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);
        String newToken = generateWebAuthToken();
        yamlConfiguration.set(ServerScopeConfigKeys.Web.AUTH_TOKEN, newToken);
        try {
            yamlConfiguration.save(configFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist regenerated ServerScope web token", exception);
        }
        return newToken;
    }

    private void ensureServerSpecificSecrets() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(configFile);
        boolean updated = false;

        String configuredToken = yamlConfiguration.getString(
                ServerScopeConfigKeys.Web.AUTH_TOKEN,
                ServerScopeConfigDefaults.WEB_AUTH_TOKEN
        );
        if (shouldGenerateWebToken(configuredToken)) {
            yamlConfiguration.set(ServerScopeConfigKeys.Web.AUTH_TOKEN, generateWebAuthToken());
            updated = true;
            logger.info("Generated a unique web auth token for this server and saved it to config.yml.");
        }

        if (!updated) {
            return;
        }

        try {
            yamlConfiguration.save(configFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist generated ServerScope secrets to config.yml", exception);
        }
    }

    public ServerScopeConfig load(ConfigurationSection configuration) {
        YamlConfigAccessor accessor = new YamlConfigAccessor(configuration, logger);
        boolean webEnabled = accessor.booleanValue(ServerScopeConfigKeys.Web.ENABLED, ServerScopeConfigDefaults.WEB_ENABLED);
        boolean webhookEnabled = accessor.booleanValue(
                ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_ENABLED,
                ServerScopeConfigDefaults.ALERTS_CHANNEL_WEBHOOK_ENABLED,
                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_WEBHOOK_ENABLED
        );

        RetentionConfig retentionConfig = new RetentionConfig(
                accessor.booleanValue(ServerScopeConfigKeys.Retention.ENABLED, ServerScopeConfigDefaults.RETENTION_ENABLED),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Retention.METRIC_SAMPLES_DAYS,
                        ServerScopeConfigDefaults.RETENTION_METRIC_SAMPLES_DAYS,
                        ServerScopeConfigKeys.Retention.LEGACY_STORAGE_RETENTION_DAYS
                ), ServerScopeConfigKeys.Retention.METRIC_SAMPLES_DAYS, ServerScopeConfigDefaults.RETENTION_METRIC_SAMPLES_DAYS),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Retention.ALERTS_DAYS,
                        ServerScopeConfigDefaults.RETENTION_ALERTS_DAYS
                ), ServerScopeConfigKeys.Retention.ALERTS_DAYS, ServerScopeConfigDefaults.RETENTION_ALERTS_DAYS),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Retention.PROFILING_DAYS,
                        ServerScopeConfigDefaults.RETENTION_PROFILING_DAYS
                ), ServerScopeConfigKeys.Retention.PROFILING_DAYS, ServerScopeConfigDefaults.RETENTION_PROFILING_DAYS),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Retention.CHUNK_SNAPSHOTS_DAYS,
                        ServerScopeConfigDefaults.RETENTION_CHUNK_SNAPSHOTS_DAYS
                ), ServerScopeConfigKeys.Retention.CHUNK_SNAPSHOTS_DAYS, ServerScopeConfigDefaults.RETENTION_CHUNK_SNAPSHOTS_DAYS)
        );

        SamplingConfig samplingConfig = new SamplingConfig(
                accessor.booleanValue(
                        ServerScopeConfigKeys.Sampling.SKIP_OVERLAPPING_CHUNK_SAMPLING,
                        ServerScopeConfigDefaults.SAMPLING_SKIP_OVERLAPPING_CHUNK_SAMPLING
                ),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Sampling.DEFAULT_MAX_CHUNKS_PER_RUN,
                        ServerScopeConfigDefaults.SAMPLING_DEFAULT_MAX_CHUNKS_PER_RUN
                ), ServerScopeConfigKeys.Sampling.DEFAULT_MAX_CHUNKS_PER_RUN, ServerScopeConfigDefaults.SAMPLING_DEFAULT_MAX_CHUNKS_PER_RUN),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Sampling.DEFAULT_INTERVAL_MILLIS,
                        ServerScopeConfigDefaults.SAMPLING_DEFAULT_INTERVAL_MILLIS
                ), ServerScopeConfigKeys.Sampling.DEFAULT_INTERVAL_MILLIS, ServerScopeConfigDefaults.SAMPLING_DEFAULT_INTERVAL_MILLIS)
        );

        StorageConfig storageConfig = new StorageConfig(
                resolveSqliteFile(accessor.stringValue(ServerScopeConfigKeys.Storage.SQLITE_FILE, ServerScopeConfigDefaults.STORAGE_SQLITE_FILE)),
                accessor.booleanValue(ServerScopeConfigKeys.Storage.ENABLED, ServerScopeConfigDefaults.STORAGE_ENABLED),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Storage.QUEUE_CAPACITY,
                        ServerScopeConfigDefaults.STORAGE_QUEUE_CAPACITY
                ), ServerScopeConfigKeys.Storage.QUEUE_CAPACITY, ServerScopeConfigDefaults.STORAGE_QUEUE_CAPACITY),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Storage.MAX_BATCH_SIZE,
                        ServerScopeConfigDefaults.STORAGE_MAX_BATCH_SIZE
                ), ServerScopeConfigKeys.Storage.MAX_BATCH_SIZE, ServerScopeConfigDefaults.STORAGE_MAX_BATCH_SIZE),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Storage.FLUSH_INTERVAL_MILLIS,
                        ServerScopeConfigDefaults.STORAGE_FLUSH_INTERVAL_MILLIS
                ), ServerScopeConfigKeys.Storage.FLUSH_INTERVAL_MILLIS, ServerScopeConfigDefaults.STORAGE_FLUSH_INTERVAL_MILLIS),
                retentionConfig.metricSamplesDays()
        );

        CollectorsConfig collectorsConfig = new CollectorsConfig(
                accessor.booleanValue(ServerScopeConfigKeys.Collectors.ENABLED, ServerScopeConfigDefaults.COLLECTORS_ENABLED),
                new ServerCollectorsConfig(
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.SERVER_TPS_MSPT_INTERVAL,
                                ServerScopeConfigDefaults.SERVER_TPS_MSPT_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.SERVER_TPS_MSPT_INTERVAL, ServerScopeConfigDefaults.SERVER_TPS_MSPT_INTERVAL_MILLIS),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.SERVER_PLAYERS_INTERVAL,
                                ServerScopeConfigDefaults.SERVER_PLAYERS_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.SERVER_PLAYERS_INTERVAL, ServerScopeConfigDefaults.SERVER_PLAYERS_INTERVAL_MILLIS),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.SERVER_JVM_INTERVAL,
                                ServerScopeConfigDefaults.SERVER_JVM_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.SERVER_JVM_INTERVAL, ServerScopeConfigDefaults.SERVER_JVM_INTERVAL_MILLIS),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.SERVER_LOADED_CHUNKS_INTERVAL,
                                ServerScopeConfigDefaults.SERVER_LOADED_CHUNKS_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.SERVER_LOADED_CHUNKS_INTERVAL, ServerScopeConfigDefaults.SERVER_LOADED_CHUNKS_INTERVAL_MILLIS),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.SERVER_TOTAL_ENTITIES_INTERVAL,
                                ServerScopeConfigDefaults.SERVER_TOTAL_ENTITIES_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.SERVER_TOTAL_ENTITIES_INTERVAL, ServerScopeConfigDefaults.SERVER_TOTAL_ENTITIES_INTERVAL_MILLIS)
                ),
                new WorldCollectorsConfig(
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.WORLD_SNAPSHOT_INTERVAL,
                                ServerScopeConfigDefaults.WORLD_SNAPSHOT_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.WORLD_SNAPSHOT_INTERVAL, ServerScopeConfigDefaults.WORLD_SNAPSHOT_INTERVAL_MILLIS),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Collectors.WORLD_CHUNK_SAMPLING_INTERVAL,
                                ServerScopeConfigDefaults.WORLD_CHUNK_SAMPLING_INTERVAL_MILLIS
                        ), ServerScopeConfigKeys.Collectors.WORLD_CHUNK_SAMPLING_INTERVAL, ServerScopeConfigDefaults.WORLD_CHUNK_SAMPLING_INTERVAL_MILLIS),
                        positiveInt(accessor.intValue(
                                ServerScopeConfigKeys.Collectors.WORLD_MAX_CHUNKS_PER_RUN,
                                samplingConfig.defaultMaxChunksPerRun()
                        ), ServerScopeConfigKeys.Collectors.WORLD_MAX_CHUNKS_PER_RUN, samplingConfig.defaultMaxChunksPerRun())
                )
        );

        WebConfig webConfig = new WebConfig(
                webEnabled,
                accessor.stringValue(ServerScopeConfigKeys.Web.HOST, ServerScopeConfigDefaults.WEB_HOST),
                boundedPort(accessor.intValue(ServerScopeConfigKeys.Web.PORT, ServerScopeConfigDefaults.WEB_PORT)),
                validateWebAuthToken(
                        webEnabled,
                        accessor.stringValue(ServerScopeConfigKeys.Web.AUTH_TOKEN, ServerScopeConfigDefaults.WEB_AUTH_TOKEN)
                ),
                accessor.booleanValue(ServerScopeConfigKeys.Web.CORS_ENABLED, ServerScopeConfigDefaults.WEB_CORS_ENABLED),
                accessor.stringValue(ServerScopeConfigKeys.Web.CORS_ALLOWED_ORIGIN, ServerScopeConfigDefaults.WEB_CORS_ALLOWED_ORIGIN),
                accessor.booleanValue(ServerScopeConfigKeys.Web.REVERSE_PROXY_ENABLED, ServerScopeConfigDefaults.WEB_REVERSE_PROXY_ENABLED),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Web.MAX_REQUESTS_PER_WINDOW,
                        ServerScopeConfigDefaults.WEB_MAX_REQUESTS_PER_WINDOW
                ), ServerScopeConfigKeys.Web.MAX_REQUESTS_PER_WINDOW, ServerScopeConfigDefaults.WEB_MAX_REQUESTS_PER_WINDOW),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Web.RATE_LIMIT_WINDOW_MILLIS,
                        ServerScopeConfigDefaults.WEB_RATE_LIMIT_WINDOW_MILLIS
                ), ServerScopeConfigKeys.Web.RATE_LIMIT_WINDOW_MILLIS, ServerScopeConfigDefaults.WEB_RATE_LIMIT_WINDOW_MILLIS),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Web.MAX_REQUEST_URI_LENGTH,
                        ServerScopeConfigDefaults.WEB_MAX_REQUEST_URI_LENGTH
                ), ServerScopeConfigKeys.Web.MAX_REQUEST_URI_LENGTH, ServerScopeConfigDefaults.WEB_MAX_REQUEST_URI_LENGTH),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Web.MAX_RESPONSE_BYTES,
                        ServerScopeConfigDefaults.WEB_MAX_RESPONSE_BYTES
                ), ServerScopeConfigKeys.Web.MAX_RESPONSE_BYTES, ServerScopeConfigDefaults.WEB_MAX_RESPONSE_BYTES)
        );

        AlertingConfig alertingConfig = new AlertingConfig(
                accessor.booleanValue(
                        ServerScopeConfigKeys.Alerts.ENABLED,
                        ServerScopeConfigDefaults.ALERTS_ENABLED,
                        ServerScopeConfigKeys.Alerts.LEGACY_ENABLED
                ),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Alerts.EVALUATION_INTERVAL_MILLIS,
                        ServerScopeConfigDefaults.ALERTS_EVALUATION_INTERVAL_MILLIS,
                        ServerScopeConfigKeys.Alerts.LEGACY_EVALUATION_INTERVAL_MILLIS
                ), ServerScopeConfigKeys.Alerts.EVALUATION_INTERVAL_MILLIS, ServerScopeConfigDefaults.ALERTS_EVALUATION_INTERVAL_MILLIS),
                nonNegativeLong(accessor.longValue(
                        ServerScopeConfigKeys.Alerts.COOLDOWN_MILLIS,
                        ServerScopeConfigDefaults.ALERTS_COOLDOWN_MILLIS,
                        ServerScopeConfigKeys.Alerts.LEGACY_COOLDOWN_MILLIS
                ), ServerScopeConfigKeys.Alerts.COOLDOWN_MILLIS, ServerScopeConfigDefaults.ALERTS_COOLDOWN_MILLIS),
                nonNegativeLong(accessor.longValue(
                        ServerScopeConfigKeys.Alerts.RATE_LIMIT_MILLIS,
                        ServerScopeConfigDefaults.ALERTS_RATE_LIMIT_MILLIS,
                        ServerScopeConfigKeys.Alerts.LEGACY_RATE_LIMIT_MILLIS
                ), ServerScopeConfigKeys.Alerts.RATE_LIMIT_MILLIS, ServerScopeConfigDefaults.ALERTS_RATE_LIMIT_MILLIS),
                new AlertThresholdsConfig(
                        positiveDouble(accessor.doubleValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_LOW_TPS,
                                ServerScopeConfigDefaults.ALERTS_LOW_TPS,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_LOW_TPS
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_LOW_TPS, ServerScopeConfigDefaults.ALERTS_LOW_TPS),
                        positiveDouble(accessor.doubleValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_MSPT,
                                ServerScopeConfigDefaults.ALERTS_HIGH_MSPT,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_HIGH_MSPT
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_MSPT, ServerScopeConfigDefaults.ALERTS_HIGH_MSPT),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_ENTITY_COUNT,
                                ServerScopeConfigDefaults.ALERTS_HIGH_ENTITY_COUNT,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_HIGH_ENTITY_COUNT
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_ENTITY_COUNT, ServerScopeConfigDefaults.ALERTS_HIGH_ENTITY_COUNT),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_CHUNK_ENTITY_COUNT,
                                ServerScopeConfigDefaults.ALERTS_HIGH_CHUNK_ENTITY_COUNT,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_HIGH_CHUNK_ENTITY_COUNT
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_CHUNK_ENTITY_COUNT, ServerScopeConfigDefaults.ALERTS_HIGH_CHUNK_ENTITY_COUNT),
                        positiveLong(accessor.longValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_CHUNK_BLOCK_ENTITY_COUNT,
                                ServerScopeConfigDefaults.ALERTS_HIGH_CHUNK_BLOCK_ENTITY_COUNT,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_HIGH_CHUNK_BLOCK_ENTITY_COUNT
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_CHUNK_BLOCK_ENTITY_COUNT, ServerScopeConfigDefaults.ALERTS_HIGH_CHUNK_BLOCK_ENTITY_COUNT),
                        positiveDouble(accessor.doubleValue(
                                ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_EVENT_AVERAGE_MILLIS,
                                ServerScopeConfigDefaults.ALERTS_HIGH_EVENT_AVERAGE_MILLIS,
                                ServerScopeConfigKeys.Alerts.LEGACY_THRESHOLD_HIGH_EVENT_AVERAGE_MILLIS
                        ), ServerScopeConfigKeys.Alerts.THRESHOLD_HIGH_EVENT_AVERAGE_MILLIS, ServerScopeConfigDefaults.ALERTS_HIGH_EVENT_AVERAGE_MILLIS)
                ),
                new AlertChannelsConfig(
                        accessor.booleanValue(
                                ServerScopeConfigKeys.Alerts.CHANNEL_CONSOLE_ENABLED,
                                ServerScopeConfigDefaults.ALERTS_CHANNEL_CONSOLE_ENABLED,
                                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_CONSOLE_ENABLED
                        ),
                        accessor.booleanValue(
                                ServerScopeConfigKeys.Alerts.CHANNEL_INGAME_ENABLED,
                                ServerScopeConfigDefaults.ALERTS_CHANNEL_INGAME_ENABLED,
                                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_INGAME_ENABLED
                        ),
                        accessor.booleanValue(
                                ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_ENABLED,
                                ServerScopeConfigDefaults.ALERTS_CHANNEL_WEBHOOK_ENABLED,
                                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_WEBHOOK_ENABLED
                        ),
                        validateWebhookUrl(
                                webhookEnabled,
                                accessor.stringValue(
                                ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL,
                                ServerScopeConfigDefaults.ALERTS_CHANNEL_WEBHOOK_URL,
                                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_WEBHOOK_URL
                                )
                        ),
                        accessor.stringValue(
                                ServerScopeConfigKeys.Alerts.CHANNEL_ADMIN_PERMISSION,
                                ServerScopeConfigDefaults.ALERTS_CHANNEL_ADMIN_PERMISSION,
                                ServerScopeConfigKeys.Alerts.LEGACY_CHANNEL_ADMIN_PERMISSION
                        )
                )
        );

        LocalizationConfig localizationConfig = new LocalizationConfig(
                accessor.stringValue(
                        ServerScopeConfigKeys.Localization.DEFAULT_LOCALE,
                        ServerScopeConfigDefaults.LOCALIZATION_DEFAULT_LOCALE
                ),
                accessor.booleanValue(
                        ServerScopeConfigKeys.Localization.LOG_MISSING_TRANSLATIONS,
                        ServerScopeConfigDefaults.LOCALIZATION_LOG_MISSING_TRANSLATIONS
                )
        );

        DebugConfig debugConfig = new DebugConfig(
                accessor.booleanValue(ServerScopeConfigKeys.Debug.ENABLED, ServerScopeConfigDefaults.DEBUG_ENABLED),
                accessor.booleanValue(ServerScopeConfigKeys.Debug.VERBOSE_LOGGING, ServerScopeConfigDefaults.DEBUG_VERBOSE_LOGGING),
                accessor.booleanValue(ServerScopeConfigKeys.Debug.LOG_CONFIG_RELOADS, ServerScopeConfigDefaults.DEBUG_LOG_CONFIG_RELOADS)
        );

        ProfilerConfig profilerConfig = new ProfilerConfig(
                accessor.booleanValue(
                        ServerScopeConfigKeys.Profiling.ENABLED,
                        ServerScopeConfigDefaults.PROFILING_ENABLED,
                        ServerScopeConfigKeys.Profiling.LEGACY_ENABLED
                ),
                accessor.stringList(
                        ServerScopeConfigKeys.Profiling.EVENTS,
                        ServerScopeConfigDefaults.PROFILING_EVENTS,
                        ServerScopeConfigKeys.Profiling.LEGACY_EVENTS
                ),
                positiveInt(accessor.intValue(
                        ServerScopeConfigKeys.Profiling.TOP_LIMIT,
                        ServerScopeConfigDefaults.PROFILING_TOP_LIMIT,
                        ServerScopeConfigKeys.Profiling.LEGACY_TOP_LIMIT
                ), ServerScopeConfigKeys.Profiling.TOP_LIMIT, ServerScopeConfigDefaults.PROFILING_TOP_LIMIT),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Profiling.BURST_WINDOW_MILLIS,
                        ServerScopeConfigDefaults.PROFILING_BURST_WINDOW_MILLIS,
                        ServerScopeConfigKeys.Profiling.LEGACY_BURST_WINDOW_MILLIS
                ), ServerScopeConfigKeys.Profiling.BURST_WINDOW_MILLIS, ServerScopeConfigDefaults.PROFILING_BURST_WINDOW_MILLIS),
                positiveLong(accessor.longValue(
                        ServerScopeConfigKeys.Profiling.BURST_MINIMUM_COUNT,
                        ServerScopeConfigDefaults.PROFILING_BURST_MINIMUM_COUNT,
                        ServerScopeConfigKeys.Profiling.LEGACY_BURST_MINIMUM_COUNT
                ), ServerScopeConfigKeys.Profiling.BURST_MINIMUM_COUNT, ServerScopeConfigDefaults.PROFILING_BURST_MINIMUM_COUNT)
        );

        return new ServerScopeConfig(
                storageConfig,
                collectorsConfig,
                webConfig,
                alertingConfig,
                localizationConfig,
                retentionConfig,
                debugConfig,
                profilerConfig,
                samplingConfig
        );
    }

    private Path resolveSqliteFile(String relativePath) {
        return resolveSqliteFile(plugin.getDataFolder().toPath(), relativePath);
    }

    static Path resolveSqliteFile(Path dataFolder, String relativePath) {
        Path normalizedDataFolder = dataFolder.toAbsolutePath().normalize();
        Path resolved = normalizedDataFolder.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedDataFolder)) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Storage.SQLITE_FILE
                    + "' must stay within plugin data folder: " + normalizedDataFolder);
        }
        return resolved;
    }

    private String validateWebAuthToken(boolean webEnabled, String authToken) {
        if (authToken.isBlank()) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Web.AUTH_TOKEN + "' must not be blank");
        }
        if (webEnabled && isPlaceholderWebToken(authToken)) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Web.AUTH_TOKEN + "' is still using an auto-generation placeholder");
        }
        return authToken;
    }

    static boolean shouldGenerateWebToken(String authToken) {
        return authToken == null || authToken.isBlank() || isPlaceholderWebToken(authToken);
    }

    static boolean isPlaceholderWebToken(String authToken) {
        return ServerScopeConfigDefaults.WEB_AUTH_TOKEN.equals(authToken)
                || LEGACY_WEB_AUTH_TOKEN_PLACEHOLDER.equals(authToken);
    }

    private String generateWebAuthToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String validateWebhookUrl(boolean webhookEnabled, String webhookUrl) {
        if (!webhookEnabled || webhookUrl.isBlank()) {
            return webhookUrl;
        }
        URI uri;
        try {
            uri = new URI(webhookUrl);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL + "' must be a valid URI", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL + "' must use https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL + "' must include a host");
        }
        if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL + "' must not include user info");
        }
        if (isLocalOrPrivateHost(uri.getHost())) {
            throw new IllegalArgumentException("Config value '" + ServerScopeConfigKeys.Alerts.CHANNEL_WEBHOOK_URL + "' must not target localhost or private network addresses");
        }
        return webhookUrl;
    }

    private static boolean isLocalOrPrivateHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost")) {
            return true;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private int positiveInt(int value, String path, int fallback) {
        if (value > 0) {
            return value;
        }
        logger.warning("Config value '" + path + "' must be positive. Using fallback '" + fallback + "'.");
        return fallback;
    }

    private long positiveLong(long value, String path, long fallback) {
        if (value > 0L) {
            return value;
        }
        logger.warning("Config value '" + path + "' must be positive. Using fallback '" + fallback + "'.");
        return fallback;
    }

    private long nonNegativeLong(long value, String path, long fallback) {
        if (value >= 0L) {
            return value;
        }
        logger.warning("Config value '" + path + "' must not be negative. Using fallback '" + fallback + "'.");
        return fallback;
    }

    private double positiveDouble(double value, String path, double fallback) {
        if (value > 0.0d) {
            return value;
        }
        logger.warning("Config value '" + path + "' must be positive. Using fallback '" + fallback + "'.");
        return fallback;
    }

    private int boundedPort(int value) {
        if (value >= 0 && value <= 65535) {
            return value;
        }
        logger.warning("Config value '" + ServerScopeConfigKeys.Web.PORT + "' must be between 0 and 65535. Using fallback '" + ServerScopeConfigDefaults.WEB_PORT + "'.");
        return ServerScopeConfigDefaults.WEB_PORT;
    }
}
