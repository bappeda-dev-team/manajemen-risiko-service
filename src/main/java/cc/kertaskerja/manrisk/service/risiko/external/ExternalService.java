package cc.kertaskerja.manrisk.service.risiko.external;

import cc.kertaskerja.manrisk.config.KertaskerjaProperties;
import cc.kertaskerja.manrisk.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class ExternalService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KertaskerjaProperties kertaskerjaProperties;

    public JsonNode getTujuanSasaran(String kodeOpd, Integer tahun) {
        try {
            return fetchSasaranApi(kodeOpd, tahun);
        } catch (HttpClientErrorException.Unauthorized e) {
            return fetchSasaranApi(kodeOpd, tahun);
        }
    }

    private JsonNode fetchSasaranApi(String kodeOpd, Integer tahun) {
        HttpHeaders headers = new HttpHeaders();

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
