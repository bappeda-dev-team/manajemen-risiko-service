package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.RisikoInternalProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RisikoInternalAuthFilterTest {
    private final RisikoInternalAuthFilter filter = new RisikoInternalAuthFilter(
          new RisikoInternalProperties("shared-secret"), sessionId -> {
              if ("valid-session".equals(sessionId)) {
                  return new RisikoAuthenticatedUser("user-session", "Session User", "OPD-001", "19870001", java.util.List.of("USER"));
              }
              if ("unavailable-session".equals(sessionId)) {
                  throw new RisikoSessionException(RisikoSessionException.Reason.UNAVAILABLE);
              }
              throw new RisikoSessionException(RisikoSessionException.Reason.UNAUTHORIZED);
          }, new ObjectMapper());

    @Test
    void rejectsRiskRequestWithoutInternalCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko");
        request.setServletPath("/risiko");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("RISK_CALLER_UNAUTHORIZED",
              new ObjectMapper().readTree(response.getContentAsString()).path("data").path("code").asText());
    }

    @Test
    void acceptsRiskRequestWithSharedTokenAndCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/risiko");
        request.setServletPath("/risiko");
        request.addHeader("Authorization", "Bearer shared-secret");
        request.addHeader(RisikoInternalAuthFilter.CALLER_HEADER, "user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("user-1", request.getAttribute(RisikoInternalAuthFilter.CALLER_ATTRIBUTE));
    }

    @Test
    void rejectsPemdaRequestWithoutInternalCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-pemda");
        request.setServletPath("/risiko-pemda");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsPemdaRequestWithoutCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/risiko-pemda");
        request.setServletPath("/risiko-pemda");
        request.addHeader("Authorization", "Bearer shared-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsPemdaRequestWithWrongToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-pemda");
        request.setServletPath("/risiko-pemda");
        request.addHeader("Authorization", "Bearer wrong-secret");
        request.addHeader(RisikoInternalAuthFilter.CALLER_HEADER, "user-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void acceptsPemdaRequestWithSharedTokenAndCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/risiko-pemda/1");
        request.setServletPath("/risiko-pemda/1");
        request.addHeader("Authorization", "Bearer shared-secret");
        request.addHeader(RisikoInternalAuthFilter.CALLER_HEADER, "user-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("user-2", request.getAttribute(RisikoInternalAuthFilter.CALLER_ATTRIBUTE));
    }

    @Test
    void rejectsOperasionalRequestWithoutInternalCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-operasional");
        request.setServletPath("/risiko-operasional");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void acceptsOperasionalRequestWithSharedTokenAndCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/risiko-operasional");
        request.setServletPath("/risiko-operasional");
        request.addHeader("Authorization", "Bearer shared-secret");
        request.addHeader(RisikoInternalAuthFilter.CALLER_HEADER, "user-3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("user-3", request.getAttribute(RisikoInternalAuthFilter.CALLER_ATTRIBUTE));
    }

    @Test
    void acceptsValidatedBrowserSessionAndIgnoresBrowserCallerHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/risiko-operasional");
        request.setServletPath("/risiko-operasional");
        request.addHeader(AuthSessionClient.SESSION_HEADER, "valid-session");
        request.addHeader(RisikoInternalAuthFilter.CALLER_HEADER, "spoofed-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("user-session", request.getAttribute(RisikoInternalAuthFilter.CALLER_ATTRIBUTE));
        assertEquals("user-session", ((RisikoAuthenticatedUser) request
              .getAttribute(RisikoInternalAuthFilter.AUTHENTICATED_USER_ATTRIBUTE)).username());
    }

    @Test
    void rejectsInvalidBrowserSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-operasional");
        request.setServletPath("/risiko-operasional");
        request.addHeader(AuthSessionClient.SESSION_HEADER, "invalid-session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("RISK_SESSION_UNAUTHORIZED",
              new ObjectMapper().readTree(response.getContentAsString()).path("data").path("code").asText());
    }

    @Test
    void surfacesUnavailableAuthServiceAs503() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-operasional");
        request.setServletPath("/risiko-operasional");
        request.addHeader(AuthSessionClient.SESSION_HEADER, "unavailable-session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(503, response.getStatus());
        assertEquals("RISK_AUTH_UNAVAILABLE",
              new ObjectMapper().readTree(response.getContentAsString()).path("data").path("code").asText());
    }
}
