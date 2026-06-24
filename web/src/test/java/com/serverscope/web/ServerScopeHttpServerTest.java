package com.serverscope.web;

import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.WebConfig;
import com.serverscope.api.storage.AlertRecord;
import com.serverscope.api.storage.AlertRepository;
import com.serverscope.api.storage.AlertSeverity;
import com.serverscope.api.storage.AlertStatus;
import com.serverscope.api.storage.MetricSample;
import com.serverscope.api.storage.MetricSampleRepository;
import com.serverscope.core.i18n.TranslationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerScopeHttpServerTest {
    private static final MetricSampleRepository EMPTY_HISTORY = new MetricSampleRepository() {
        @Override
        public List<MetricSample> findLatestMetricSamples(int limit) {
            return List.of();
        }

        @Override
        public List<MetricSample> findMetricSamplesSince(Instant since, int limit) {
            return List.of();
        }
    };

    private ServerScopeHttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthEndpointRequiresAuthToken() throws Exception {
        server = newServer(EMPTY_HISTORY);
        server.start();

        HttpResponse<String> response = get("/health", null);
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("UNAUTHORIZED"));
    }

    @Test
    void historyEndpointReturnsPersistedSamplesForAuthorizedRequests() throws Exception {
        Instant base = Instant.now().minusSeconds(120);
        MetricSampleRepository history = new MetricSampleRepository() {
            @Override
            public List<MetricSample> findLatestMetricSamples(int limit) {
                return List.of();
            }

            @Override
            public List<MetricSample> findMetricSamplesSince(Instant since, int limit) {
                return List.of(
                        new MetricSample(base, 19.9, 11.2, 1024L, 3, 1, 50L, 200L),
                        new MetricSample(base.plusSeconds(60), 18.4, 22.5, 2048L, 4, 1, 60L, 240L)
                );
            }
        };
        server = newServer(history);
        server.start();

        HttpResponse<String> response = get("/api/history?minutes=30", "secret-token");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"windowMinutes\":30"), response.body());
        assertTrue(response.body().contains("\"points\""), response.body());
        assertTrue(response.body().contains("19.9"), response.body());
    }

    @Test
    void alertHistoryEndpointReturnsPersistedAlerts() throws Exception {
        AlertRepository alerts = limit -> List.of(
                new AlertRecord(Instant.now(), "LOW_TPS", AlertSeverity.WARN, AlertStatus.ACTIVE,
                        "server", "TPS below threshold", "{}"),
                new AlertRecord(Instant.now(), "HOT_CHUNK", AlertSeverity.CRITICAL, AlertStatus.RESOLVED,
                        "world:1:2", "Chunk cooled down", "{}")
        );
        server = newServer(EMPTY_HISTORY, alerts);
        server.start();

        HttpResponse<String> response = get("/api/alerts/history?limit=50", "secret-token");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("LOW_TPS"), response.body());
        assertTrue(response.body().contains("\"status\":\"RESOLVED\""), response.body());

        HttpResponse<String> filtered = get("/api/alerts/history?status=ACTIVE", "secret-token");
        assertEquals(200, filtered.statusCode());
        assertTrue(filtered.body().contains("LOW_TPS"), filtered.body());
        assertTrue(!filtered.body().contains("HOT_CHUNK"), filtered.body());
    }

    @Test
    void historyEndpointRejectsInvalidMinutes() throws Exception {
        server = newServer(EMPTY_HISTORY);
        server.start();

        HttpResponse<String> response = get("/api/history?minutes=abc", "secret-token");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("BAD_REQUEST"), response.body());
    }

    private ServerScopeHttpServer newServer(MetricSampleRepository history) {
        return newServer(history, limit -> List.of());
    }

    private ServerScopeHttpServer newServer(MetricSampleRepository history, AlertRepository alertHistory) {
        return new ServerScopeHttpServer(
                Logger.getLogger("test"),
                new WebConfig(true, "127.0.0.1", 0, "secret-token", false, "", false, 60, 10_000L, 2048, 1_048_576),
                new TranslationService(Logger.getLogger("test-i18n"), new LocalizationConfig("en", false)),
                Map::of,
                () -> null,
                List::of,
                List::of,
                history,
                alertHistory
        );
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + server.localPort() + path))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
