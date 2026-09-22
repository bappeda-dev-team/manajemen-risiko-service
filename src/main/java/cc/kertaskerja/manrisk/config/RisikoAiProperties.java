package cc.kertaskerja.manrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manrisk.ai")
public record RisikoAiProperties(
        boolean enabled,
        OpenRouter openrouter,
        String internalToken,
        int rateLimitPerMinute,
        int maxConcurrentRequests,
        int connectTimeoutSeconds,
        int requestTimeoutSeconds,
        int providerTimeoutSeconds
) {
    public record OpenRouter(String baseUrl, String apiKey, String model) {}

    public boolean isConfigured() {
        return enabled
                && openrouter != null
                && hasText(openrouter.apiKey())
                && hasText(openrouter.baseUrl())
                && hasText(openrouter.model())
                && hasText(internalToken);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("ISI_");
    }
}
