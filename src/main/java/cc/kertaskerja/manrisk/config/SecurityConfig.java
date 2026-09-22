package cc.kertaskerja.manrisk.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import cc.kertaskerja.manrisk.security.RisikoAiInternalAuthFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "kertaskerja.security", name = "mode", havingValue = "none")
    SecurityFilterChain noSecurity(HttpSecurity http, RisikoAiInternalAuthFilter risikoAiInternalAuthFilter) throws Exception {

        http
              .cors(Customizer.withDefaults())
              .csrf(AbstractHttpConfigurer::disable)
              .addFilterBefore(risikoAiInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "kertaskerja.security", name = "mode", havingValue = "resource-server")
    SecurityFilterChain resourceServer(HttpSecurity http, RisikoAiInternalAuthFilter risikoAiInternalAuthFilter) throws Exception {

        http
              .cors(Customizer.withDefaults())
              .csrf(AbstractHttpConfigurer::disable)
              .addFilterBefore(risikoAiInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
              .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

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
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
