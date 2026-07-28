package cc.kertaskerja.manrisk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                      .allowedOrigins("http://localhost:3000", "https://manrisk-test.zeabur.app")
                      .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                      .allowedHeaders("*")
                      .allowCredentials(true);
            }
        };
    }
}
