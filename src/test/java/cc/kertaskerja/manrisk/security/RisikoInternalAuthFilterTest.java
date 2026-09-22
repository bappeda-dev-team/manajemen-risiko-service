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
          new RisikoInternalProperties("shared-secret"), new ObjectMapper());

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
}
