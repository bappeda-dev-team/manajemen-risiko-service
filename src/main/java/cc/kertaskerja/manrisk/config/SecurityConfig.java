package cc.kertaskerja.manrisk.config;

import cc.kertaskerja.manrisk.security.MahuluCookieAuthenticationFilter;
import cc.kertaskerja.manrisk.security.MahuluSessionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
          HttpSecurity http,
          MahuluSessionClient sessionClient,
          ObjectMapper objectMapper) throws Exception {

        http
              .cors(Customizer.withDefaults())
              .csrf(AbstractHttpConfigurer::disable)
              .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated())
              .addFilterBefore(new MahuluCookieAuthenticationFilter(sessionClient, objectMapper),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "kertaskerja.cors", name = "enabled", havingValue = "true")
    CorsConfigurationSource corsConfigurationSource(
          KertaskerjaProperties kertaskerjaProperties) {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(kertaskerjaProperties.cors().allowedHosts());
        config.setAllowedMethods(List.of(
              "GET",
              "POST",
              "PUT",
              "PATCH",
              "DELETE",
              "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
