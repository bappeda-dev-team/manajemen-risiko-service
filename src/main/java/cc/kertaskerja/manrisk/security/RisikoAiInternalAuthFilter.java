package cc.kertaskerja.manrisk.security;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
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

@Component
@RequiredArgsConstructor
public class RisikoAiInternalAuthFilter extends OncePerRequestFilter {
    public static final String CALLER_ATTRIBUTE = "risikoAiCaller";

    private final RisikoAiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/risiko/generate-ai".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.internalToken();
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String caller = request.getHeader("X-AI-User-Id");
        String supplied = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()) : "";

        if (!hasText(expected) || !constantTimeEquals(expected, supplied) || !hasText(caller)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiResponse.error(401, java.util.Map.of("code", "AI_CALLER_UNAUTHORIZED"),
                            "Unauthorized AI caller"));
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
