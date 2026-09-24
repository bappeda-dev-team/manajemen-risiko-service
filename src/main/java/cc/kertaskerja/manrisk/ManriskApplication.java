package cc.kertaskerja.manrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import cc.kertaskerja.manrisk.config.KertaskerjaProperties;
import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.config.RisikoInternalProperties;
import cc.kertaskerja.manrisk.config.AuthServiceProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({KertaskerjaProperties.class, RisikoAiProperties.class, RisikoInternalProperties.class,
        AuthServiceProperties.class})
public class ManriskApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManriskApplication.class, args);
	}

}
