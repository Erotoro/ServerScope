package com.serverscope.analyzer.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.config.AlertChannelsConfig;
import com.serverscope.core.security.SecretMasker;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class WebhookAlertNotifier implements AlertNotifier {
    private final Logger logger;
    private final AlertChannelsConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebhookAlertNotifier(Logger logger, AlertChannelsConfig config) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void notify(AlertRecord alertRecord) {
        if (!config.webhookEnabled() || config.webhookUrl().isBlank()) {
            return;
        }

        try {
            URI webhookUri = validateWebhookUri(config.webhookUrl());
            String payload = serializePayload(alertRecord);
            HttpRequest request = HttpRequest.newBuilder(webhookUri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "ServerScope-MVP")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            CompletableFuture<HttpResponse<Void>> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    logger.warning("Failed to send alert webhook: " + SecretMasker.mask(throwable));
                    return;
                }
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    logger.warning("Alert webhook responded with HTTP " + response.statusCode());
                }
            });
        } catch (RuntimeException exception) {
            logger.warning("Failed to prepare alert webhook request: " + SecretMasker.mask(exception));
        }
    }

    private URI validateWebhookUri(String webhookUrl) {
        URI uri;
        try {
            uri = new URI(webhookUrl);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Webhook URL is not a valid URI", exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            throw new IllegalArgumentException("Webhook URL must use https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Webhook URL must include a host");
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalized) || normalized.endsWith(".localhost")) {
            throw new IllegalArgumentException("Webhook URL must not target localhost");
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Webhook URL must not target private or local network addresses");
            }
        } catch (UnknownHostException ignored) {
            // Leave unresolved hostnames to normal HTTP client validation.
        }
        return uri;
    }

    private String serializePayload(AlertRecord alertRecord) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "ServerScope");
        payload.put("code", alertRecord.code());
        payload.put("dedupeKey", alertRecord.dedupeKey());
        payload.put("severity", alertRecord.severity().name());
        payload.put("status", alertRecord.status().name());
        payload.put("message", alertRecord.message());
        payload.put("occurredAt", alertRecord.occurredAt());
        payload.put("labels", alertRecord.labels());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize webhook payload", exception);
        }
    }
}
