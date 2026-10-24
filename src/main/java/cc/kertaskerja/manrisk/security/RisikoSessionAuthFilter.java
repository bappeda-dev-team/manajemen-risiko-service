package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RisikoSessionAuthFilter extends OncePerRequestFilter {
    public static final String SESSION_HEADER = "X-Session-Id";
    public static final String CALLER_ATTRIBUTE = "risikoCaller";
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
              || !("/risiko".equals(path) || path.startsWith("/risiko/")
              || "/risiko-opd".equals(path) || path.startsWith("/risiko-opd/")
              || "/risiko-pemda".equals(path) || path.startsWith("/risiko-pemda/")
              || "/risiko-operasional".equals(path) || path.startsWith("/risiko-operasional/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getHeader(SESSION_HEADER);
        if (!hasText(sessionId)) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_SESSION_REQUIRED",
                  "Risk session is required");
            return;
        }

        // The auth service has no session lookup endpoint yet. Keep the session value out of logs and rate-limit keys.
        request.setAttribute(CALLER_ATTRIBUTE, callerKey(sessionId));
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(status, Map.of("code", code), message));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("ISI_");
    }

    private static String callerKey(String sessionId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                  .digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return "session-" + HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
