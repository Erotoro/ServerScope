package com.serverscope.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.api.profile.ProfilerSnapshot;
import com.serverscope.api.storage.AlertRepository;
import com.serverscope.api.storage.MetricSampleRepository;
import com.serverscope.web.api.AlertHistoryItemResponse;
import com.serverscope.web.api.HistoryPointResponse;
import com.serverscope.web.api.HistoryResponse;
import com.serverscope.core.concurrent.NamedThreadFactory;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.security.SecretMasker;
import com.serverscope.web.api.ApiEnvelope;
import com.serverscope.web.api.ChunkItemResponse;
import com.serverscope.web.api.HealthResponse;
import com.serverscope.web.api.MetricItemResponse;
import com.serverscope.web.api.MetricValueResponse;
import com.serverscope.web.api.OverviewResponse;
import com.serverscope.web.api.ProfilingResponse;
import com.serverscope.web.api.WorldItemResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerScopeHttpServer implements WebServerControl {
    private static final int DEFAULT_QUERY_LIMIT = 100;
    private static final int MAX_QUERY_LIMIT = 500;
    private static final int DEFAULT_HISTORY_MINUTES = 30;
    private static final int MAX_HISTORY_MINUTES = 1440;
    private static final int MAX_STATIC_RESOURCE_BYTES = 262_144;
    private static final int GLOBAL_REQUEST_BUDGET_MULTIPLIER = 4;
    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'";

    private final Logger logger;
    private final WebConfig config;
    private final TranslationService translations;
    private final Supplier<Map<String, MetricBatch>> batchesSupplier;
    private final Supplier<ProfilerSnapshot> profilerSnapshotSupplier;
    private final Supplier<List<AlertRecord>> activeAlertsSupplier;
    private final Supplier<List<DiagnosticFinding>> activeFindingsSupplier;
    private final MetricSampleRepository historyRepository;
    private final AlertRepository alertHistoryRepository;
    private final ObjectMapper objectMapper;
    private final Object rateLimitLock = new Object();
    private final Map<String, FixedWindowCounter> perIpCounters = new LinkedHashMap<>();
    private FixedWindowCounter globalCounter = new FixedWindowCounter(0L, 0);

    private volatile HttpServer server;
    private volatile ExecutorService executor;

    public ServerScopeHttpServer(
            Logger logger,
            WebConfig config,
            TranslationService translations,
            Supplier<Map<String, MetricBatch>> batchesSupplier,
            Supplier<ProfilerSnapshot> profilerSnapshotSupplier,
            Supplier<List<AlertRecord>> activeAlertsSupplier,
            Supplier<List<DiagnosticFinding>> activeFindingsSupplier,
            MetricSampleRepository historyRepository,
            AlertRepository alertHistoryRepository
    ) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.translations = Objects.requireNonNull(translations, "translations");
        this.batchesSupplier = Objects.requireNonNull(batchesSupplier, "batchesSupplier");
        this.profilerSnapshotSupplier = Objects.requireNonNull(profilerSnapshotSupplier, "profilerSnapshotSupplier");
        this.activeAlertsSupplier = Objects.requireNonNull(activeAlertsSupplier, "activeAlertsSupplier");
        this.activeFindingsSupplier = Objects.requireNonNull(activeFindingsSupplier, "activeFindingsSupplier");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository");
        this.alertHistoryRepository = Objects.requireNonNull(alertHistoryRepository, "alertHistoryRepository");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        HttpServer httpServer = null;
        ExecutorService executorService = null;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(config.host(), config.port()), 0);
            executorService = Executors.newFixedThreadPool(4,
                    NamedThreadFactory.daemonNumbered("serverscope-web"));
            httpServer.setExecutor(executorService);
            httpServer.createContext("/", this::handleStatic);
            httpServer.createContext("/assets/", this::handleStatic);
            httpServer.createContext("/health", withJson(true, this::health));
            httpServer.createContext("/api/overview", withJson(true, this::overview));
            httpServer.createContext("/api/history", withJson(true, this::history));
            httpServer.createContext("/api/metrics", withJson(true, this::metrics));
            httpServer.createContext("/api/worlds", withJson(true, this::worlds));
            httpServer.createContext("/api/chunks", withJson(true, this::chunks));
            httpServer.createContext("/api/profiling", withJson(true, this::profiling));
            httpServer.createContext("/api/plugins", withJson(true, this::profiling));
            httpServer.createContext("/api/findings", withJson(true, this::findings));
            httpServer.createContext("/api/alerts/history", withJson(true, this::alertHistory));
            httpServer.createContext("/api/alerts", withJson(true, this::alerts));
            httpServer.start();
            this.executor = executorService;
            this.server = httpServer;
        } catch (IOException | RuntimeException exception) {
            if (httpServer != null) {
                httpServer.stop(0);
            }
            if (executorService != null) {
                executorService.shutdownNow();
            }
            throw new IllegalStateException("Failed to start embedded HTTP server", exception);
        }
    }

    public synchronized void stop() {
        HttpServer currentServer = server;
        ExecutorService currentExecutor = executor;
        server = null;
        executor = null;

        if (currentServer != null) {
            currentServer.stop(1);
        }
        if (currentExecutor != null) {
            currentExecutor.shutdown();
            try {
                if (!currentExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    currentExecutor.shutdownNow();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                currentExecutor.shutdownNow();
                logger.log(Level.WARNING, "Interrupted while stopping web executor", exception);
            }
        }
    }

    private HttpHandler withJson(boolean authRequired, ExchangeResponder responder) {
        return exchange -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    handleOptions(exchange);
                    return;
                }
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    writeJson(exchange, 405, ApiEnvelope.failure("METHOD_NOT_ALLOWED", text(exchange, "web.api.error.method_not_allowed")));
                    return;
                }
                enforceRequestConstraints(exchange);
                if (!allowRequest(exchange)) {
                    writeJson(exchange, 429, ApiEnvelope.failure("RATE_LIMITED", text(exchange, "web.api.error.rate_limited")));
                    return;
                }
                if (authRequired && !isAuthorized(exchange)) {
                    writeJson(exchange, 401, ApiEnvelope.failure("UNAUTHORIZED", text(exchange, "web.api.error.unauthorized")));
                    return;
                }
                writeJson(exchange, 200, ApiEnvelope.success(responder.respond(exchange)));
            } catch (IllegalArgumentException exception) {
                writeJson(exchange, 400, ApiEnvelope.failure("BAD_REQUEST", exception.getMessage()));
            } catch (Exception exception) {
                logger.warning(translations.text("web.log.handler_failed") + ": " + SecretMasker.mask(exception));
                writeJson(exchange, 500, ApiEnvelope.failure("INTERNAL_ERROR", text(exchange, "web.api.error.internal")));
            } finally {
                exchange.close();
            }
        };
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String resourcePath;
            if ("/".equals(path) || path.isBlank()) {
                resourcePath = "web/index.html";
            } else if (path.startsWith("/assets/")) {
                resourcePath = "web" + path;
            } else {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            try (InputStream inputStream = ServerScopeHttpServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (inputStream == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] bytes = inputStream.readAllBytes();
                if (bytes.length > MAX_STATIC_RESOURCE_BYTES) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                applySecurityHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", contentType(resourcePath));
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bytes);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private HealthResponse health(HttpExchange exchange) {
        return new HealthResponse(
                "UP",
                Instant.now()
        );
    }

    private OverviewResponse overview(HttpExchange exchange) {
        Map<String, MetricBatch> batches = batchesSupplier.get();
        ProfilerSnapshot profilerSnapshot = profilerSnapshotSupplier.get();
        Instant capturedAt = profilerSnapshot == null ? Instant.now() : profilerSnapshot.capturedAt();
        return new OverviewResponse(
                capturedAt,
                flattenServerMetrics(batches),
                slice(activeAlertsSupplier.get(), queryLimit(exchange)),
                slice(activeFindingsSupplier.get(), queryLimit(exchange)),
                profilerSnapshot == null ? List.of() : slice(profilerSnapshot.topSlowEvents(), queryLimit(exchange)),
                profilerSnapshot == null ? List.of() : slice(profilerSnapshot.topFrequentEvents(), queryLimit(exchange)),
                profilerSnapshot == null ? List.of() : slice(profilerSnapshot.topSuspiciousBursts(), queryLimit(exchange)),
                profilerSnapshot == null ? List.of() : slice(profilerSnapshot.topPlugins(), queryLimit(exchange))
        );
    }

    private HistoryResponse history(HttpExchange exchange) {
        int minutes = queryMinutes(exchange);
        int limit = queryLimit(exchange);
        Instant since = Instant.now().minus(java.time.Duration.ofMinutes(minutes));

        List<HistoryPointResponse> points = historyRepository.findMetricSamplesSince(since, limit).stream()
                .map(sample -> new HistoryPointResponse(
                        sample.sampleTime(),
                        sample.tps(),
                        sample.mspt(),
                        sample.heapUsedBytes(),
                        sample.onlinePlayers(),
                        sample.worldCount(),
                        sample.loadedChunks(),
                        sample.totalEntities()
                ))
                .toList();
        return new HistoryResponse(Instant.now(), since, minutes, points);
    }

    private List<MetricItemResponse> metrics(HttpExchange exchange) {
        String collectorIdFilter = queryParam(exchange.getRequestURI(), "collectorId");
        String metricTypeFilter = queryParam(exchange.getRequestURI(), "metricType");
        int limit = queryLimit(exchange);

        return batchesSupplier.get().values().stream()
                .sorted(Comparator.comparing(MetricBatch::collectorId))
                .filter(batch -> collectorIdFilter == null || batch.collectorId().equalsIgnoreCase(collectorIdFilter))
                .flatMap(batch -> batch.samples().stream().map(sample -> toMetricJson(batch.collectorId(), sample)))
                .filter(item -> metricTypeFilter == null || item.metricType().equalsIgnoreCase(metricTypeFilter))
                .limit(limit)
                .toList();
    }

    private List<WorldItemResponse> worlds(HttpExchange exchange) {
        String worldFilter = queryParam(exchange.getRequestURI(), "world");
        int limit = queryLimit(exchange);

        return batchesSupplier.get().values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type().name().equals("LOADED_CHUNKS"))
                .filter(sample -> sample.labels().get("world") != null)
                .map(sample -> new WorldItemResponse(
                        sample.labels().get("world"),
                        sample.longValue(),
                        sample.timestamp()
                ))
                .filter(item -> worldFilter == null || item.world().equalsIgnoreCase(worldFilter))
                .sorted(Comparator.comparing(WorldItemResponse::timestamp).reversed())
                .limit(limit)
                .toList();
    }

    private List<ChunkItemResponse> chunks(HttpExchange exchange) {
        String worldFilter = queryParam(exchange.getRequestURI(), "world");
        int limit = queryLimit(exchange);

        Map<String, ChunkItemResponse> chunks = new LinkedHashMap<>();
        for (MetricBatch batch : batchesSupplier.get().values()) {
            for (MetricSample sample : batch.samples()) {
                String world = sample.labels().get("world");
                String chunkX = sample.labels().get("chunk_x");
                String chunkZ = sample.labels().get("chunk_z");
                if (world == null || chunkX == null || chunkZ == null) {
                    continue;
                }
                if (worldFilter != null && !world.equalsIgnoreCase(worldFilter)) {
                    continue;
                }

                String key = world + ":" + chunkX + ":" + chunkZ;
                ChunkItemResponse current = chunks.get(key);
                long entityCount = current == null ? 0L : current.entityCount();
                long blockEntityCount = current == null ? 0L : current.blockEntityCount();
                Instant timestamp = current == null ? sample.timestamp() : laterOf(current.timestamp(), sample.timestamp());
                Integer parsedChunkX = parseChunkCoordinate(chunkX);
                Integer parsedChunkZ = parseChunkCoordinate(chunkZ);
                if (parsedChunkX == null || parsedChunkZ == null) {
                    continue;
                }

                if (sample.type().name().equals("ENTITY_COUNT")) {
                    entityCount = sample.longValue();
                }
                if (sample.type().name().equals("BLOCK_ENTITY_COUNT")) {
                    blockEntityCount = sample.longValue();
                }

                chunks.put(key, new ChunkItemResponse(
                        world,
                        parsedChunkX,
                        parsedChunkZ,
                        entityCount,
                        blockEntityCount,
                        timestamp
                ));
            }
        }

        return chunks.values().stream()
                .sorted(Comparator.comparingLong((ChunkItemResponse item) -> item.entityCount() * 10L + item.blockEntityCount() * 25L).reversed())
                .limit(limit)
                .toList();
    }

    private ProfilingResponse profiling(HttpExchange exchange) {
        ProfilerSnapshot profilerSnapshot = profilerSnapshotSupplier.get();
        if (profilerSnapshot == null) {
            return new ProfilingResponse(Instant.now(), List.of(), List.of(), List.of(), List.of());
        }
        int limit = queryLimit(exchange);
        return new ProfilingResponse(
                profilerSnapshot.capturedAt(),
                slice(profilerSnapshot.topPlugins(), limit),
                slice(profilerSnapshot.topSlowEvents(), limit),
                slice(profilerSnapshot.topFrequentEvents(), limit),
                slice(profilerSnapshot.topSuspiciousBursts(), limit)
        );
    }

    private List<DiagnosticFinding> findings(HttpExchange exchange) {
        int limit = queryLimit(exchange);
        String severity = queryParam(exchange.getRequestURI(), "severity");
        return activeFindingsSupplier.get().stream()
                .filter(item -> severity == null || item.severity().name().equalsIgnoreCase(severity))
                .sorted(Comparator.comparing(DiagnosticFinding::timestamp).reversed())
                .limit(limit)
                .toList();
    }

    private List<AlertRecord> alerts(HttpExchange exchange) {
        int limit = queryLimit(exchange);
        String severity = queryParam(exchange.getRequestURI(), "severity");
        String status = queryParam(exchange.getRequestURI(), "status");
        return activeAlertsSupplier.get().stream()
                .filter(item -> severity == null || item.severity().name().equalsIgnoreCase(severity))
                .filter(item -> status == null || item.status().name().equalsIgnoreCase(status))
                .sorted(Comparator.comparing(AlertRecord::occurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private List<AlertHistoryItemResponse> alertHistory(HttpExchange exchange) {
        int limit = queryLimit(exchange);
        String severity = queryParam(exchange.getRequestURI(), "severity");
        String status = queryParam(exchange.getRequestURI(), "status");
        return alertHistoryRepository.findLatestAlerts(limit).stream()
                .filter(item -> severity == null || item.severity().name().equalsIgnoreCase(severity))
                .filter(item -> status == null || item.status().name().equalsIgnoreCase(status))
                .map(item -> new AlertHistoryItemResponse(
                        item.eventTime(),
                        item.alertCode(),
                        item.severity().name(),
                        item.status().name(),
                        item.dedupeKey(),
                        item.message()
                ))
                .toList();
    }

    private Map<String, MetricValueResponse> flattenServerMetrics(Map<String, MetricBatch> batches) {
        Map<String, MetricValueResponse> result = new LinkedHashMap<>();
        for (MetricBatch batch : batches.values()) {
            for (MetricSample sample : batch.samples()) {
                if (!sample.labels().isEmpty()) {
                    continue;
                }
                result.put(sample.type().name(), new MetricValueResponse(sample.numericValue(), sample.timestamp()));
            }
        }
        return result;
    }

    private MetricItemResponse toMetricJson(String collectorId, MetricSample sample) {
        return new MetricItemResponse(
                collectorId,
                sample.type().name(),
                sample.numericValue(),
                sample.timestamp(),
                sample.labels().values()
        );
    }

    private boolean isAuthorized(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return constantTimeEquals(authorization.substring("Bearer ".length()), config.authToken());
        }
        String tokenHeader = exchange.getRequestHeaders().getFirst("X-ServerScope-Token");
        return tokenHeader != null && constantTimeEquals(tokenHeader, config.authToken());
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        applySecurityHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    private void writeJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(payload);
        if (bytes.length > config.maxResponseBytes()) {
            byte[] limited = objectMapper.writeValueAsBytes(ApiEnvelope.failure("RESPONSE_TOO_LARGE", text(exchange, "web.api.error.response_too_large")));
            applySecurityHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(413, limited.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(limited);
            }
            return;
        }
        applySecurityHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String contentType(String resourcePath) {
        if (resourcePath.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (resourcePath.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (resourcePath.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private void applySecurityHeaders(HttpExchange exchange) {
        applyCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        if (config.reverseProxyEnabled() && isForwardedHttps(exchange)) {
            exchange.getResponseHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    private void applyCorsHeaders(HttpExchange exchange) {
        if (!config.corsEnabled() || config.corsAllowedOrigin().isBlank()) {
            return;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", config.corsAllowedOrigin());
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, X-ServerScope-Token, Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
    }

    private int queryMinutes(HttpExchange exchange) {
        String value = queryParam(exchange.getRequestURI(), "minutes");
        if (value == null || value.isBlank()) {
            return DEFAULT_HISTORY_MINUTES;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Query param 'minutes' must be positive");
            }
            return Math.min(parsed, MAX_HISTORY_MINUTES);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Query param 'minutes' must be a valid integer");
        }
    }

    private int queryLimit(HttpExchange exchange) {
        String value = queryParam(exchange.getRequestURI(), "limit");
        if (value == null || value.isBlank()) {
            return DEFAULT_QUERY_LIMIT;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Query param 'limit' must be positive");
            }
            return Math.min(parsed, MAX_QUERY_LIMIT);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Query param 'limit' must be a valid integer");
        }
    }

    private void enforceRequestConstraints(HttpExchange exchange) {
        String requestUri = exchange.getRequestURI().toASCIIString();
        if (requestUri.length() > config.maxRequestUriLength()) {
            throw new IllegalArgumentException(text(exchange, "web.api.error.request_too_large"));
        }
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                long length = Long.parseLong(contentLength);
                if (length > 0L) {
                    throw new IllegalArgumentException(text(exchange, "web.api.error.request_too_large"));
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(text(exchange, "web.api.error.bad_request"));
            }
        }
    }

    private boolean allowRequest(HttpExchange exchange) {
        long now = System.currentTimeMillis();
        String clientKey = clientIp(exchange);
        synchronized (rateLimitLock) {
            globalCounter = nextCounter(globalCounter, now, config.rateLimitWindowMillis());
            if (globalCounter.count() >= config.maxRequestsPerWindow() * GLOBAL_REQUEST_BUDGET_MULTIPLIER) {
                return false;
            }
            FixedWindowCounter current = perIpCounters.get(clientKey);
            current = nextCounter(current, now, config.rateLimitWindowMillis());
            if (current.count() >= config.maxRequestsPerWindow()) {
                perIpCounters.put(clientKey, current);
                return false;
            }
            perIpCounters.put(clientKey, current.increment());
            globalCounter = globalCounter.increment();
            trimCounters(now);
            return true;
        }
    }

    private FixedWindowCounter nextCounter(FixedWindowCounter current, long now, long windowMillis) {
        if (current == null || now - current.windowStartedAt() >= windowMillis) {
            return new FixedWindowCounter(now, 0);
        }
        return current;
    }

    private void trimCounters(long now) {
        Deque<String> expired = new ArrayDeque<>();
        for (Map.Entry<String, FixedWindowCounter> entry : perIpCounters.entrySet()) {
            if (now - entry.getValue().windowStartedAt() >= config.rateLimitWindowMillis()) {
                expired.add(entry.getKey());
            }
        }
        while (!expired.isEmpty()) {
            perIpCounters.remove(expired.removeFirst());
        }
    }

    private String queryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            if (!key.equals(name)) {
                continue;
            }
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return null;
    }

    private <T> List<T> slice(List<T> list, int limit) {
        if (list.size() <= limit) {
            return list;
        }
        return list.subList(0, limit);
    }

    private Instant laterOf(Instant left, Instant right) {
        return right.isAfter(left) ? right : left;
    }

    private Integer parseChunkCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String text(HttpExchange exchange, String key) {
        return translations.text(requestLocale(exchange), key);
    }

    private String requestLocale(HttpExchange exchange) {
        String queryLocale = queryParam(exchange.getRequestURI(), "lang");
        if (queryLocale != null && !queryLocale.isBlank()) {
            return translations.normalizeLocale(queryLocale);
        }
        String acceptLanguage = exchange.getRequestHeaders().getFirst("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return translations.defaultLocale();
        }
        String first = acceptLanguage.split(",")[0].trim();
        return translations.normalizeLocale(first);
    }

    int localPort() {
        HttpServer current = server;
        if (current == null) {
            return config.port();
        }
        return current.getAddress().getPort();
    }

    private String clientIp(HttpExchange exchange) {
        if (config.reverseProxyEnabled()) {
            String forwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return exchange.getRemoteAddress().getAddress() == null
                ? exchange.getRemoteAddress().toString()
                : exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private boolean isForwardedHttps(HttpExchange exchange) {
        String forwardedProto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.equalsIgnoreCase("https");
    }

    @FunctionalInterface
    private interface ExchangeResponder {
        Object respond(HttpExchange exchange);
    }

    private record FixedWindowCounter(long windowStartedAt, int count) {
        private FixedWindowCounter increment() {
            return new FixedWindowCounter(windowStartedAt, count + 1);
        }
    }
}
