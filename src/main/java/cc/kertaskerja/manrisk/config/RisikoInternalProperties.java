package cc.kertaskerja.manrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manrisk.internal")
public record RisikoInternalProperties(String token) {
}
