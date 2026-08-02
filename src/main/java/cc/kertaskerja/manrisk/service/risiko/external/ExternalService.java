package cc.kertaskerja.manrisk.service.risiko.external;

import cc.kertaskerja.manrisk.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kertaskerja.api.base-url}")
    private String baseUrl;

    @Value("${kertaskerja.api.username}")
    private String username;

    @Value("${kertaskerja.api.password}")
    private String password;

    public JsonNode getTujuanSasaran(String kodeOpd, Integer tahun) {
        String sessionId = login();
        return getTujuanSasaran(sessionId, kodeOpd, tahun);
    }

    private String login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
              "username", username,
              "password", password
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
              baseUrl + "/auth/login",
              HttpMethod.POST,
              request,
              String.class
        );

        return parseJson(response.getBody()).get("sessionId").asText();
    }

    private JsonNode getTujuanSasaran(String sessionId, String kodeOpd, Integer tahun) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Session-Id", sessionId);
        headers.set(HttpHeaders.COOKIE, "sessionId=" + sessionId);

        String url = UriComponentsBuilder.fromUriString(baseUrl + "/api/v1/penetapan/opd/tujuan-with-sasaran")
              .queryParam("kodeOpd", kodeOpd)
              .queryParam("tahun", tahun)
              .build()
              .toUriString();

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
              url,
              HttpMethod.GET,
              request,
              String.class
        );

        return parseJson(response.getBody());
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to parse external API response", e);
        }
    }
}
