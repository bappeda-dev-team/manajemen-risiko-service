package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.MahuluAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MahuluSessionClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void forwardsCookieAndMapsMahuluUserInfoToPrincipal() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/user/user-info", exchange -> {
            assertEquals("mahulu_session=valid", exchange.getRequestHeaders().getFirst("Cookie"));
            byte[] body = "{\"username\":\"pegawai_1\",\"firstName\":\"Pegawai Satu\",\"kode_opd\":\"OPD-001\",\"roles\":[\"level_3\"]}"
                  .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        MahuluSessionClient client = new MahuluSessionClient(
              new MahuluAuthProperties(baseUrl, 1, 2), new ObjectMapper());

        RiskPrincipal principal = client.resolve("mahulu_session=valid");

        assertEquals("pegawai_1", principal.id());
        assertEquals("Pegawai Satu", principal.nama());
        assertEquals("OPD-001", principal.kodeOpd());
        assertEquals(true, principal.hasRole("level_3"));
    }
}
