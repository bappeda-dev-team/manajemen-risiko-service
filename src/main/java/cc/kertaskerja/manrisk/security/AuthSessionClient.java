package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.AuthServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthSessionClient implements RisikoSessionValidator {
    public static final String SESSION_HEADER = "X-Session-Id";

    private final AuthServiceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    @Autowired
    public AuthSessionClient(AuthServiceProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(timeout(properties.connectTimeoutSeconds())))
              .build());
    }

    AuthSessionClient(AuthServiceProperties properties, ObjectMapper objectMapper, HttpClient client) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    @Override
    public RisikoAuthenticatedUser validate(String sessionId) {
        if (!properties.isConfigured()) {
            throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(userInfoUri())
                  .timeout(Duration.ofSeconds(timeout(properties.requestTimeoutSeconds())))
                  .header(SESSION_HEADER, sessionId)
                  .GET()
                  .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new RisikoSessionException(RisikoSessionException.Reason.UNAUTHORIZED);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE);
            }
            return parseUser(response.body());
        } catch (RisikoSessionException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE, exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE, exception);
        }
    }

    private URI userInfoUri() {
        String normalized = properties.baseUrl().trim().replaceAll("/+$", "");
        return URI.create(normalized + "/user-info");
    }

    private RisikoAuthenticatedUser parseUser(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode user = root.path("data").isObject() ? root.path("data") : root;
            String username = text(user, "username");
            String nip = text(user, "nip");
            if (!hasText(username) && !hasText(nip)) {
                throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE);
            }
            return new RisikoAuthenticatedUser(username, text(user, "firstName"), text(user, "kode_opd"), nip,
                  roles(user.path("roles")));
        } catch (RisikoSessionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE, exception);
        }
    }

    private static List<String> roles(JsonNode source) {
        if (!source.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        source.forEach(node -> {
            if (node.isTextual() && !node.asText().isBlank()) result.add(node.asText().trim());
        });
        return List.copyOf(result);
    }

    private static String text(JsonNode source, String name) {
        JsonNode value = source.path(name);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static long timeout(int value) {
        return Math.max(1, value);
    }
}
