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

    // Mengambil variable dari .env
    @Value("${AUTH_PENETAPAN}")
    private String authUrl;

    @Value("${USERNAME_API_EXTERNAL}")
    private String usernameExternal;

    @Value("${PASSWORD_API_EXTERNAL}")
    private String passwordExternal;

    // Variabel untuk menyimpan sessionId agar tidak perlu login terus-menerus
    private String cachedSessionId = "";

    public JsonNode getTujuanSasaran(String kodeOpd, Integer tahun) {
        // Ambil session ID (login jika belum ada session)
        String sessionId = getSessionId();

        try {
            return fetchSasaranApi(sessionId, kodeOpd, tahun);
        } catch (HttpClientErrorException.Unauthorized e) {
            // Jika ternyata session expired (401 Unauthorized), reset session dan coba login ulang 1x
            System.out.println("Session API Eksternal expired, mencoba login ulang...");
            this.cachedSessionId = "";
            sessionId = getSessionId();
            return fetchSasaranApi(sessionId, kodeOpd, tahun);
        }
    }

    private synchronized String getSessionId() {
        // Jika session sudah ada di memori, langsung gunakan
        if (cachedSessionId != null && !cachedSessionId.isBlank()) {
            return cachedSessionId;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Body request JSON
        Map<String, String> body = new HashMap<>();
        body.put("username", usernameExternal);
        body.put("password", passwordExternal);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                  authUrl,
                  HttpMethod.POST,
                  request,
                  String.class
            );

            // Ekstrak sessionId dari header Set-Cookie
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.contains("sessionId=")) {
                        // Memotong string cookie untuk mengambil valuenya saja
                        String[] parts = cookie.split(";");
                        for (String part : parts) {
                            if (part.trim().startsWith("sessionId=")) {
                                this.cachedSessionId = part.trim().substring("sessionId=".length());
                                return this.cachedSessionId;
                            }
                        }
                    }
                }
            }

            // JIKA ternyata sessionId dibalikan di dalam body JSON (bukan cookie), aktifkan kode di bawah ini:
            // JsonNode jsonNode = parseJson(response.getBody());
            // if (jsonNode.has("sessionId")) {
            //     this.cachedSessionId = jsonNode.get("sessionId").asText();
            //     return this.cachedSessionId;
            // }

        } catch (Exception ignored) {
            // Credential and session details must not be written to application logs.
        }

        return "";
    }

    private JsonNode fetchSasaranApi(String sessionId, String kodeOpd, Integer tahun) {
        HttpHeaders headers = new HttpHeaders();
        // Memasukkan sessionId ke dalam cookie request
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
