package cc.kertaskerja.manrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import cc.kertaskerja.manrisk.config.KertaskerjaProperties;
import cc.kertaskerja.manrisk.config.MahuluAuthProperties;
import cc.kertaskerja.manrisk.config.RisikoAiProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({KertaskerjaProperties.class, MahuluAuthProperties.class, RisikoAiProperties.class})
public class ManriskApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManriskApplication.class, args);
	}

}
