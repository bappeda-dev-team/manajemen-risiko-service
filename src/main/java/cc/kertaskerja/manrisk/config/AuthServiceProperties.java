package cc.kertaskerja.manrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manrisk.auth")
public record AuthServiceProperties(
      String baseUrl,
      int connectTimeoutSeconds,
      int requestTimeoutSeconds) {

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
