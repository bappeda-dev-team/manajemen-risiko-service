package cc.kertaskerja.manrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kertaskerja.security")
public record SecurityProperties(
      Mode mode
) {

    public enum Mode {
        NONE,
        RESOURCE_SERVER
    }
}
