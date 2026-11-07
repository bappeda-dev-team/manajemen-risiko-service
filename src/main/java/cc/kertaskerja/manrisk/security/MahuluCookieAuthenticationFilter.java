package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

public class MahuluCookieAuthenticationFilter extends OncePerRequestFilter {
    private final MahuluSessionClient sessionClient;
    private final ObjectMapper objectMapper;

    public MahuluCookieAuthenticationFilter(MahuluSessionClient sessionClient, ObjectMapper objectMapper) {
        this.sessionClient = sessionClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
              || path.startsWith("/actuator/")
              || path.startsWith("/swagger-ui")
              || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
        try {
            RiskPrincipal principal = sessionClient.resolve(request.getHeader("Cookie"));
            if (principal == null) throw new MahuluSessionClient.SessionInvalidException();
            var authorities = principal.roles().stream().map(SimpleGrantedAuthority::new).toList();
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (MahuluSessionClient.SessionInvalidException exception) {
            write(response, HttpStatus.UNAUTHORIZED, "AUTH_SESSION_INVALID", "Sesi Mahulu tidak valid atau telah berakhir.");
        } catch (MahuluSessionClient.AuthServiceUnavailableException exception) {
            write(response, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SERVICE_UNAVAILABLE", "Layanan autentikasi Mahulu sedang tidak tersedia.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(status.value(), Map.of("code", code), message));
    }
}
