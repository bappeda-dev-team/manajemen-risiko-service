package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Component
public class RisikoSessionAuthFilter extends OncePerRequestFilter {
    public static final String SESSION_HEADER = "X-Session-Id";
    public static final String CALLER_ATTRIBUTE = "risikoCaller";
    private static final String LOCAL_DEV_CALLER = "local-dev-unverified";

    private final ObjectMapper objectMapper;
    private final boolean allowUnverifiedLocal;

    public RisikoSessionAuthFilter(ObjectMapper objectMapper,
                                   @Value("${manrisk.auth.allow-unverified-local:false}") boolean allowUnverifiedLocal) {
        this.objectMapper = objectMapper;
        this.allowUnverifiedLocal = allowUnverifiedLocal;
    }

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

        if (!allowUnverifiedLocal) {
            reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "RISK_AUTH_UNAVAILABLE",
                  "Validasi sesi risiko belum dikonfigurasi.");
            return;
        }

        if (!isLoopback(request.getRemoteAddr())) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "RISK_LOCAL_ONLY",
                  "Mode sesi tanpa verifikasi hanya tersedia dari localhost.");
            return;
        }

        // Temporary local development mode: the header is not an authenticated identity.
        request.setAttribute(CALLER_ATTRIBUTE, LOCAL_DEV_CALLER);
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

    private static boolean isLoopback(String address) {
        if (address == null || address.isBlank()) return false;
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
