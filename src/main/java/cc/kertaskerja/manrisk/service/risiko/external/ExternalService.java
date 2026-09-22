package cc.kertaskerja.manrisk.service.risiko.external;

import cc.kertaskerja.manrisk.config.KertaskerjaProperties;
import cc.kertaskerja.manrisk.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KertaskerjaProperties kertaskerjaProperties;

    @Value("${AUTH_PENETAPAN:}")
    private String authUrl;

    @Value("${USERNAME_API_EXTERNAL:}")
    private String usernameExternal;

    @Value("${PASSWORD_API_EXTERNAL:}")
    private String passwordExternal;

    private String cachedSessionId = "";

    public JsonNode getTujuanSasaran(String kodeOpd, Integer tahun) {
        String sessionId = getSessionId();
        try {
            return fetchSasaranApi(sessionId, kodeOpd, tahun);
        } catch (HttpClientErrorException.Unauthorized e) {
            cachedSessionId = "";
            return fetchSasaranApi(getSessionId(), kodeOpd, tahun);
        }
    }

    private synchronized String getSessionId() {
        if (!cachedSessionId.isBlank()) return cachedSessionId;
        if (authUrl.isBlank() || usernameExternal.isBlank() || passwordExternal.isBlank()) {
            throw new ServiceException("Penetapan API authentication is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("username", usernameExternal);
        body.put("password", passwordExternal);

        ResponseEntity<String> response = restTemplate.exchange(
              authUrl,
              HttpMethod.POST,
              new HttpEntity<>(body, headers),
              String.class
        );

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                for (String part : cookie.split(";")) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("sessionId=")) {
                        cachedSessionId = trimmed.substring("sessionId=".length());
                        if (!cachedSessionId.isBlank()) return cachedSessionId;
                    }
                }
            }
        }
        throw new ServiceException("Penetapan API login did not return a session");
    }

    private JsonNode fetchSasaranApi(String sessionId, String kodeOpd, Integer tahun) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, "sessionId=" + sessionId);

        String url = UriComponentsBuilder.fromUriString(kertaskerjaProperties.api().penetapan().baseUrl() + "/opd/tujuan-with-sasaran")
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
