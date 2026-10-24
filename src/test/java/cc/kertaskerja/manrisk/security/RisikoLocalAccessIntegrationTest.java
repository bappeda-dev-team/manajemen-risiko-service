package cc.kertaskerja.manrisk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "manrisk.auth.allow-unverified-local=true")
@Testcontainers(disabledWithoutDocker = true)
class RisikoLocalAccessIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Value("${local.server.port}")
    private int port;

    @Test
    void laporanIsReachableFromLoopbackWithoutUserInfoLookup() throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + port
              + "/manrisk/api/risiko-opd/laporan?tahun=2026&kodeOpd=OPD-TEST");
        HttpRequest request = HttpRequest.newBuilder(uri)
              .timeout(Duration.ofSeconds(5))
              .header(RisikoSessionAuthFilter.SESSION_HEADER, "test-local-session")
              .GET()
              .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
              .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"data\":[]"), response.body());
    }

    @Test
    void laporanStillRequiresHeaderInLocalMode() throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + port
              + "/manrisk/api/risiko-pemda/laporan?tahun=2026");
        HttpRequest request = HttpRequest.newBuilder(uri)
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
              .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode(), response.body());
        assertTrue(response.body().contains("RISK_SESSION_REQUIRED"), response.body());
    }
}
