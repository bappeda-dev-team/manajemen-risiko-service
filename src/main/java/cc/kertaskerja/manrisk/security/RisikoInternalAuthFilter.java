package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.RisikoInternalProperties;
import cc.kertaskerja.manrisk.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RisikoInternalAuthFilter extends OncePerRequestFilter {
    public static final String CALLER_ATTRIBUTE = "risikoCaller";
    public static final String CALLER_HEADER = "X-Manrisk-User-Id";

    private final RisikoInternalProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
              || !("/risiko".equals(path) || path.startsWith("/risiko/")
              || "/risiko-pemda".equals(path) || path.startsWith("/risiko-pemda/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.token();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String caller = request.getHeader(CALLER_HEADER);
        String supplied = authorization != null && authorization.startsWith("Bearer ")
              ? authorization.substring("Bearer ".length()) : "";

        if (!hasText(expected) || !constantTimeEquals(expected, supplied) || !hasText(caller)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                  ApiResponse.error(401, Map.of("code", "RISK_CALLER_UNAUTHORIZED"),
                        "Unauthorized risk caller"));
            return;
        }

        request.setAttribute(CALLER_ATTRIBUTE, caller.trim());
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
              supplied.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("ISI_");
    }
}
