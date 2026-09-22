package cc.kertaskerja.manrisk.service.risiko.external;

import cc.kertaskerja.manrisk.config.ApiProperties;
import cc.kertaskerja.manrisk.config.CorsProperties;
import cc.kertaskerja.manrisk.config.KertaskerjaProperties;
import cc.kertaskerja.manrisk.config.SecurityProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalServiceTest {

    private static final String AUTH_URL = "https://penetapan.test/auth/login";
    private static final String BASE_URL = "https://penetapan.test";

    @Test
    void logsInAndUsesReturnedSessionForSasaranRequest() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ExternalService service = new ExternalService(restTemplate, properties());
        ReflectionTestUtils.setField(service, "authUrl", AUTH_URL);
        ReflectionTestUtils.setField(service, "usernameExternal", "service-user");
        ReflectionTestUtils.setField(service, "passwordExternal", "service-password");

        server.expect(requestTo(AUTH_URL))
              .andExpect(method(POST))
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
              .andExpect(content().json("{" + "\"username\":\"service-user\",\"password\":\"service-password\"}"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.SET_COOKIE, "sessionId=session-123; Path=/; HttpOnly"));
        server.expect(requestTo(BASE_URL + "/opd/tujuan-with-sasaran?kodeOpd=OPD-001&tahun=2026"))
              .andExpect(method(GET))
              .andExpect(header(HttpHeaders.COOKIE, "sessionId=session-123"))
              .andRespond(withSuccess("{\"data\":{\"kode_opd\":\"OPD-001\"}}", MediaType.APPLICATION_JSON));

        JsonNode response = service.getTujuanSasaran("OPD-001", 2026);

        assertEquals("OPD-001", response.path("data").path("kode_opd").asText());
        server.verify();
    }

    private KertaskerjaProperties properties() {
        return new KertaskerjaProperties(
              new ApiProperties(new ApiProperties.PenetapanApi(BASE_URL)),
              "x.xx",
              "MANAJEMEN-RISIKO-UP",
              new SecurityProperties(SecurityProperties.Mode.NONE),
              new CorsProperties(false, List.of())
        );
    }
}
