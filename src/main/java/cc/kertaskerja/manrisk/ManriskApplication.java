package cc.kertaskerja.manrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ManriskApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManriskApplication.class, args);
	}

}
