package cc.kertaskerja.manrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manrisk.auth")
public record MahuluAuthProperties(
      String baseUrl,
      int connectTimeoutSeconds,
      int requestTimeoutSeconds
) { }
