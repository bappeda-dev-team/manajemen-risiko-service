package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.AuthServiceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSessionClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final AuthSessionClient client = new AuthSessionClient(
          new AuthServiceProperties("https://auth.example.test/", 3, 3), new ObjectMapper(), httpClient);

    @Test
    void acceptsEnvelopedUserAndForwardsSessionToUserInfo() throws Exception {
        respond(200, """
              {"data":{"username":"operator","nip":"123","firstName":"Nama","kode_opd":"OPD-1","roles":["USER"]}}
              """);

        RisikoAuthenticatedUser user = client.validate("session-123");

        assertEquals("operator", user.callerId());
        assertEquals("OPD-1", user.kodeOpd());
        assertEquals(List.of("USER"), user.roles());
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any());
        assertEquals("https://auth.example.test/user-info", request.getValue().uri().toString());
        assertEquals("session-123", request.getValue().headers().firstValue(AuthSessionClient.SESSION_HEADER).orElseThrow());
    }

    @Test
    void acceptsRootUserWithUsernameOnly() throws Exception {
        respond(200, "{\"username\":\"operator\"}");
        assertEquals("operator", client.validate("session-123").callerId());
    }

    @Test
    void acceptsNipOnlyWhenUsernameMissing() throws Exception {
        respond(200, "{\"data\":{\"nip\":\"123\"}}");
        assertEquals("123", client.validate("session-123").callerId());
    }

    @Test
    void missingIdentityIsAuthUnavailable() throws Exception {
        respond(200, "{\"data\":{\"firstName\":\"Nama\"}}");
        assertEquals(RisikoSessionException.Reason.UNAVAILABLE,
              assertThrows(RisikoSessionException.class, () -> client.validate("session-123")).reason());
    }

    @Test
    void unauthorizedAndServerFailureStayDistinct() throws Exception {
        respond(401, "{}");
        assertEquals(RisikoSessionException.Reason.UNAUTHORIZED,
              assertThrows(RisikoSessionException.class, () -> client.validate("session-123")).reason());

        respond(503, "{}");
        assertEquals(RisikoSessionException.Reason.UNAVAILABLE,
              assertThrows(RisikoSessionException.class, () -> client.validate("session-123")).reason());
    }

    @SuppressWarnings("unchecked")
    private void respond(int status, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
}
