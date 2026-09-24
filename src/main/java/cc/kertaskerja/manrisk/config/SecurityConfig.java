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
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import cc.kertaskerja.manrisk.security.RisikoInternalAuthFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(prefix = "kertaskerja.security", name = "mode", havingValue = "none")
    SecurityFilterChain noSecurity(HttpSecurity http, RisikoInternalAuthFilter risikoInternalAuthFilter) throws Exception {

        http
              .cors(Customizer.withDefaults())
              .csrf(AbstractHttpConfigurer::disable)
              .addFilterBefore(risikoInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "kertaskerja.security", name = "mode", havingValue = "resource-server")
    SecurityFilterChain resourceServer(HttpSecurity http, RisikoInternalAuthFilter risikoInternalAuthFilter) throws Exception {
        DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

        http
              .cors(Customizer.withDefaults())
              .csrf(AbstractHttpConfigurer::disable)
              .addFilterBefore(risikoInternalAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/risiko", "/risiko/**", "/risiko-pemda", "/risiko-pemda/**").permitAll()
                    .requestMatchers("/risiko-operasional", "/risiko-operasional/**").permitAll()
                    .anyRequest().authenticated())
              .oauth2ResourceServer(oauth -> oauth
                    // /risiko memakai shared internal bearer token, bukan JWT user.
                    .bearerTokenResolver(request -> {
                        String path = request.getServletPath();
                        if ("/risiko".equals(path) || path.startsWith("/risiko/")
                              || "/risiko-pemda".equals(path) || path.startsWith("/risiko-pemda/")) return null;
                        return bearerTokenResolver.resolve(request);
                    })
                    .jwt(Customizer.withDefaults()));

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
        config.setAllowedHeaders(List.of("Content-Type", "X-Session-Id", "Authorization", "X-Manrisk-User-Id"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
