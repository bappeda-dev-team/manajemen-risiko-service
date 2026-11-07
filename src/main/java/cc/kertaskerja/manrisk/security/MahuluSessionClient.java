package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.MahuluAuthProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MahuluSessionClient {
    private final MahuluAuthProperties properties;
    private final ObjectMapper objectMapper;

    public MahuluSessionClient(MahuluAuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public RiskPrincipal resolve(String cookie) {
        if (cookie == null || cookie.isBlank()) throw new SessionInvalidException();
        String baseUrl = normalized(properties.baseUrl());
        if (baseUrl == null) throw new AuthServiceUnavailableException("URL layanan autentikasi belum dikonfigurasi.");
        baseUrl = baseUrl.replaceFirst("/+$", "");

        try {
            HttpClient client = HttpClient.newBuilder()
                  .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                  .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/user/user-info"))
                  .timeout(Duration.ofSeconds(properties.requestTimeoutSeconds()))
                  .header("Accept", "application/json")
                  .header("Cookie", cookie)
                  .GET()
                  .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) throw new SessionInvalidException();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthServiceUnavailableException("Layanan autentikasi merespons status " + response.statusCode() + ".");
            }
            MahuluUserInfo user = objectMapper.readValue(response.body(), MahuluUserInfo.class);
            String id = normalized(user.username());
            if (id == null) throw new AuthServiceUnavailableException("Respons autentikasi tidak memuat username.");
            String nama = normalized(user.firstName());
            Set<String> roles = user.roles() == null ? Set.of() : user.roles().stream()
                  .map(MahuluSessionClient::normalized)
                  .filter(value -> value != null)
                  .map(value -> value.toLowerCase(Locale.ROOT))
                  .collect(Collectors.toUnmodifiableSet());
            return new RiskPrincipal(id, nama == null ? id : nama, null, normalized(user.kodeOpd()), roles);
        } catch (SessionInvalidException | AuthServiceUnavailableException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthServiceUnavailableException("Permintaan autentikasi terputus.");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthServiceUnavailableException("Layanan autentikasi tidak dapat dihubungi.");
        }
    }

    private static String normalized(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MahuluUserInfo(String username, String firstName,
                                  @JsonProperty("kode_opd") String kodeOpd, Set<String> roles) { }

    public static class SessionInvalidException extends RuntimeException { }
    public static class AuthServiceUnavailableException extends RuntimeException {
        public AuthServiceUnavailableException(String message) { super(message); }
    }
}
