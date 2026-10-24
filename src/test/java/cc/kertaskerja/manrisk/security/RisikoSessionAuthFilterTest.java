package cc.kertaskerja.manrisk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RisikoSessionAuthFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RisikoSessionAuthFilter filter = new RisikoSessionAuthFilter(objectMapper);

    @Test
    void requiresSessionOnAllRiskPrefixes() throws Exception {
        for (String path : List.of("/risiko", "/risiko/generate-ai", "/risiko-opd", "/risiko-opd/1",
              "/risiko-opd/laporan", "/risiko-pemda/laporan", "/risiko-pemda",
              "/risiko-pemda/1", "/risiko-operasional", "/risiko-operasional/1")) {
            MockHttpServletRequest request = riskRequest(path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(401, response.getStatus(), path);
            assertEquals("RISK_SESSION_REQUIRED", errorCode(response), path);
        }
    }

    @Test
    void bearerAndSpoofedCallerCannotReplaceSession() throws Exception {
        MockHttpServletRequest request = riskRequest("/risiko-pemda/1");
        request.addHeader("Authorization", "Bearer shared-secret");
        request.addHeader("X-Manrisk-User-Id", "spoofed-user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("RISK_SESSION_REQUIRED", errorCode(response));
        assertNull(request.getAttribute(RisikoSessionAuthFilter.CALLER_ATTRIBUTE));
    }

    @Test
    void acceptsPresentSessionAndIgnoresBrowserIdentityHeaders() throws Exception {
        MockHttpServletRequest request = riskRequest("/risiko-operasional");
        request.addHeader(RisikoSessionAuthFilter.SESSION_HEADER, "valid-session");
        request.addHeader("X-Manrisk-User-Id", "spoofed-user");
        request.addHeader("Authorization", "Bearer shared-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        String caller = (String) request.getAttribute(RisikoSessionAuthFilter.CALLER_ATTRIBUTE);
        assertNotNull(caller);
        assertNotEquals("valid-session", caller);
        assertNotEquals("spoofed-user", caller);
    }

    @Test
    void sameSessionProducesSameCallerKey() throws Exception {
        MockHttpServletRequest request = riskRequest("/risiko");
        request.addHeader(RisikoSessionAuthFilter.SESSION_HEADER, "same-session");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        MockHttpServletRequest repeated = riskRequest("/risiko-pemda");
        repeated.addHeader(RisikoSessionAuthFilter.SESSION_HEADER, "same-session");
        filter.doFilter(repeated, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(request.getAttribute(RisikoSessionAuthFilter.CALLER_ATTRIBUTE),
              repeated.getAttribute(RisikoSessionAuthFilter.CALLER_ATTRIBUTE));
    }

    @Test
    void rejectsPlaceholderSessionHeader() throws Exception {
        MockHttpServletRequest request = riskRequest("/risiko-opd/laporan");
        request.addHeader(RisikoSessionAuthFilter.SESSION_HEADER, "ISI_SESSION_ID");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("RISK_SESSION_REQUIRED", errorCode(response));
    }

    @Test
    void skipsOptionsAndUnrelatedRoutes() throws Exception {
        MockHttpServletRequest preflight = riskRequest("/risiko-pemda");
        preflight.setMethod("OPTIONS");
        MockHttpServletResponse preflightResponse = new MockHttpServletResponse();
        filter.doFilter(preflight, preflightResponse, new MockFilterChain());
        assertEquals(200, preflightResponse.getStatus());

        MockHttpServletRequest unrelated = riskRequest("/actuator/health");
        MockHttpServletResponse unrelatedResponse = new MockHttpServletResponse();
        filter.doFilter(unrelated, unrelatedResponse, new MockFilterChain());
        assertEquals(200, unrelatedResponse.getStatus());
    }

    private static MockHttpServletRequest riskRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private String errorCode(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString()).path("data").path("code").asText();
    }
}
