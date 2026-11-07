package cc.kertaskerja.manrisk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MahuluCookieAuthenticationFilterTest {
    private final MahuluSessionClient sessionClient = mock(MahuluSessionClient.class);
    private final MahuluCookieAuthenticationFilter filter = new MahuluCookieAuthenticationFilter(sessionClient, new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingOrInvalidCookieReturnsStableUnauthorizedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-pemda");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("{\"status\":401,\"message\":\"Sesi Mahulu tidak valid atau telah berakhir.\",\"data\":{\"code\":\"AUTH_SESSION_INVALID\"}}",
              response.getContentAsString());
    }

    @Test
    void validCookieBuildsPrincipalOnlyForTheCurrentRequest() throws Exception {
        when(sessionClient.resolve("mahulu_session=valid"))
              .thenReturn(new RiskPrincipal("pegawai_1", "Pegawai Satu", null, "OPD-001", Set.of("level_3")));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/risiko-opd");
        request.addHeader("Cookie", "mahulu_session=valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) {
                assertEquals("pegawai_1", RiskPrincipalContext.current().id());
            }
        };

        filter.doFilter(request, response, chain);

        assertNull(RiskPrincipalContext.current());
    }
}
