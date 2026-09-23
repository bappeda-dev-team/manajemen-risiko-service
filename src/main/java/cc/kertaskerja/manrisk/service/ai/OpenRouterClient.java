package cc.kertaskerja.manrisk.service.ai;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient {
    private static final int MAX_RESPONSE_BYTES = 131_072;
    private final RisikoAiProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode generate(RisikoAiPromptFactory.Prompt prompt, Duration timeout) {
        URI uri = chatCompletionsUri();
        Map<String, Object> payload = Map.of(
                "model", properties.openrouter().model(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt.system()),
                        Map.of("role", "user", "content", prompt.user())),
                "response_format", Map.of("type", "json_schema", "json_schema", Map.of(
                        "name", "risiko_ai_result", "strict", true, "schema", prompt.schema())),
                "provider", Map.of("require_parameters", true));

        try {
            String body = objectMapper.writeValueAsString(payload);
            log.info("OpenRouter request: endpoint={}, model={}, apiKeyConfigured={}, timeoutSeconds={}",
                    uri,
                    properties.openrouter().model(),
                    hasText(properties.openrouter().apiKey()),
                    timeout.toSeconds());
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + properties.openrouter().apiKey())
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(1, properties.connectTimeoutSeconds())))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("OpenRouter response: endpoint={}, model={}, status={}, responseBytes={}",
                    uri,
                    properties.openrouter().model(),
                    response.statusCode(),
                    response.body() == null ? 0 : response.body().length());
            if (response.body() != null && response.body().length() > MAX_RESPONSE_BYTES) {
                throw new AiException(502, "AI_INVALID_OUTPUT", "Respons AI terlalu besar.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenRouter rejected request: endpoint={}, model={}, status={}, providerError={}",
                        uri,
                        properties.openrouter().model(),
                        response.statusCode(),
                        providerError(response.body()));
                if (response.statusCode() == 401 || response.statusCode() == 402 || response.statusCode() == 403 || response.statusCode() == 429) {
                    throw new AiException(503, "AI_PROVIDER_UNAVAILABLE", "Layanan AI belum tersedia.");
                }
                throw new AiException(502, "AI_PROVIDER_ERROR", "Layanan AI mengalami gangguan.");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new AiException(502, "AI_INVALID_OUTPUT", "AI tidak mengembalikan usulan yang dapat digunakan.");
            }
            return objectMapper.readTree(content);
        } catch (AiException exception) {
            throw exception;
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new AiException(504, "AI_TIMEOUT", "Waktu generate AI habis.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiException(504, "AI_TIMEOUT", "Generate AI dibatalkan.", exception);
        } catch (Exception exception) {
            log.error("OpenRouter request failed: endpoint={}, model={}, cause={}",
                    uri,
                    properties.openrouter().model(),
                    exception.getClass().getSimpleName(),
                    exception);
            throw new AiException(502, "AI_PROVIDER_ERROR", "Layanan AI mengalami gangguan.", exception);
        }
    }

    private String providerError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "empty response";
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            String code = cleanLogValue(error.path("code").asText("unknown"));
            String message = cleanLogValue(error.path("message").asText("no provider message"));
            return "code=" + code + ", message=" + truncate(message, 500);
        } catch (Exception ignored) {
            return "non-JSON response";
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String cleanLogValue(String value) {
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "…";
    }

    private URI chatCompletionsUri() {
        try {
            String baseUrl = properties.openrouter().baseUrl().replaceAll("/+$", "");
            URI base = URI.create(baseUrl);
            if (!"https".equalsIgnoreCase(base.getScheme()) || !"openrouter.ai".equalsIgnoreCase(base.getHost())) {
                throw new AiException(503, "AI_NOT_CONFIGURED", "Konfigurasi layanan AI tidak valid.");
            }
            return URI.create(baseUrl + "/chat/completions");
        } catch (IllegalArgumentException exception) {
            throw new AiException(503, "AI_NOT_CONFIGURED", "Konfigurasi layanan AI tidak valid.", exception);
        }
    }
}
