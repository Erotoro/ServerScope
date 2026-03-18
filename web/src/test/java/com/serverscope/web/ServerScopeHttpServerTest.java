package com.serverscope.web;

import com.serverscope.api.config.LocalizationConfig;
import com.serverscope.api.config.WebConfig;
import com.serverscope.core.i18n.TranslationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerScopeHttpServerTest {
    private ServerScopeHttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthEndpointRequiresAuthToken() throws Exception {
        server = new ServerScopeHttpServer(
                Logger.getLogger("test"),
                new WebConfig(true, "127.0.0.1", 0, "secret-token", false, "", false, 60, 10_000L, 2048, 1_048_576),
                new TranslationService(Logger.getLogger("test-i18n"), new LocalizationConfig("en", false)),
                Map::of,
                () -> null,
                List::of,
                List::of
        );
        server.start();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.localPort() + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("UNAUTHORIZED"));
    }
}
