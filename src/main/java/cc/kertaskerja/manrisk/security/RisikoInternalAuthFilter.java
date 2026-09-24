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
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "risikoAuthenticatedUser";
    public static final String CALLER_HEADER = "X-Manrisk-User-Id";

    private final RisikoInternalProperties properties;
    private final RisikoSessionValidator sessionValidator;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
              || !("/risiko".equals(path) || path.startsWith("/risiko/")
              || "/risiko-pemda".equals(path) || path.startsWith("/risiko-pemda/")
              || "/risiko-operasional".equals(path) || path.startsWith("/risiko-operasional/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.token();
        String sessionId = request.getHeader(AuthSessionClient.SESSION_HEADER);
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String caller = request.getHeader(CALLER_HEADER);
        String supplied = authorization != null && authorization.startsWith("Bearer ")
              ? authorization.substring("Bearer ".length()) : "";

        if (hasText(sessionId)) {
            try {
                RisikoAuthenticatedUser user = sessionValidator.validate(sessionId);
                if (!hasText(user.callerId())) {
                    reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_SESSION_UNAUTHORIZED",
                          "Unauthorized risk session");
                    return;
                }
                request.setAttribute(CALLER_ATTRIBUTE, user.callerId());
                request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
                filterChain.doFilter(request, response);
                return;
            } catch (RisikoSessionException exception) {
                if (exception.reason() == RisikoSessionException.Reason.UNAUTHORIZED) {
                    reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_SESSION_UNAUTHORIZED",
                          "Unauthorized risk session");
                } else {
                    reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "RISK_AUTH_UNAVAILABLE",
                          "Layanan autentikasi belum dapat dihubungi.");
                }
                return;
            }
        }

        if (!hasText(expected) || !constantTimeEquals(expected, supplied) || !hasText(caller)) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_CALLER_UNAUTHORIZED",
                  "Unauthorized risk caller");
            return;
        }

        request.setAttribute(CALLER_ATTRIBUTE, caller.trim());
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(status, Map.of("code", code), message));
    }

    private static boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
              supplied.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("ISI_");
    }
}
