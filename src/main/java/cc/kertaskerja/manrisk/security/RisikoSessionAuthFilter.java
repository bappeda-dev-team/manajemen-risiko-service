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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RisikoSessionAuthFilter extends OncePerRequestFilter {
    public static final String CALLER_ATTRIBUTE = "risikoCaller";
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "risikoAuthenticatedUser";
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
        String sessionId = request.getHeader(AuthSessionClient.SESSION_HEADER);
        if (!hasText(sessionId)) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_SESSION_REQUIRED",
                  "Risk session is required");
            return;
        }

        try {
            RisikoAuthenticatedUser user = sessionValidator.validate(sessionId);
            if (user == null || !hasText(user.callerId())) {
                reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "RISK_AUTH_UNAVAILABLE",
                      "Layanan autentikasi belum dapat dihubungi.");
                return;
            }
            request.setAttribute(CALLER_ATTRIBUTE, user.callerId());
            request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user);
            filterChain.doFilter(request, response);
        } catch (RisikoSessionException exception) {
            if (exception.reason() == RisikoSessionException.Reason.UNAUTHORIZED) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "RISK_SESSION_UNAUTHORIZED",
                      "Unauthorized risk session");
            } else {
                reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "RISK_AUTH_UNAVAILABLE",
                      "Layanan autentikasi belum dapat dihubungi.");
            }
        }
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(status, Map.of("code", code), message));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !value.startsWith("ISI_");
    }
}
