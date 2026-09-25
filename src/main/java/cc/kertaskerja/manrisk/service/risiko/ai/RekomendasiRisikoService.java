package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiResDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RekomendasiRisikoService {
    private final RisikoAiProperties properties;
    private final RisikoAiRequestGuard requestGuard;
    private final RisikoAiContextService contextService;
    private final RisikoAiPromptFactory promptFactory;
    private final OpenRouterClient openRouterClient;
    private final RisikoAiOutputValidator outputValidator;
    private final ObjectMapper objectMapper;

    public GenerateAiResDTO generate(String caller, GenerateAiReqDTO request) {
        validateRequest(request);
        Instant started = Instant.now();
        String safeType = "unknown";
        String scope = "unknown";
        String templateId = "unresolved";
        String templateVersion = "unresolved";
        String contextHash = "unresolved";
        String status = "success";
        try {
            if (!properties.enabled()) throw new AiException(503, "AI_DISABLED", "Fitur generate AI belum diaktifkan.");
            if (!properties.isConfigured()) throw new AiException(503, "AI_NOT_CONFIGURED", "Konfigurasi layanan AI belum lengkap.");
            Instant deadline = Instant.now().plusSeconds(Math.max(1, properties.requestTimeoutSeconds()));
            try (RisikoAiRequestGuard.Permit ignored = requestGuard.acquire(caller, request.requestId())) {
                RisikoAiContextService.ResolvedContext context = contextService.normalize(request.context());
                scope = context.value().path("scope").asText("unknown");
                contextHash = context.hash();
                Duration remaining = remaining(deadline);
                RisikoAiPromptFactory.Prompt prompt = promptFactory.build(request, context.value());
                safeType = request.type();
                templateId = prompt.templateId();
                templateVersion = prompt.templateVersion();
                JsonNode output = openRouterClient.generate(prompt,
                      Duration.ofSeconds(Math.min(Math.max(1, properties.providerTimeoutSeconds()), Math.max(1, remaining.toSeconds()))));
                JsonNode normalized = outputValidator.normalize(request.type(), output);
                // Do not expose JsonNode in an API DTO: non-Jackson serializers render its
                // Java bean metadata instead of the generated JSON payload.
                Object result = objectMapper.convertValue(normalized, Object.class);
                return new GenerateAiResDTO(request.requestId(), request.type(), context.hash(), result,
                      properties.openrouter().model());
            }
        } catch (AiException exception) {
            status = exception.getCode();
            throw exception;
        } catch (RuntimeException exception) {
            status = "AI_INTERNAL_ERROR";
            throw exception;
        } finally {
            log.info("AI generation: requestId={}, type={}, scope={}, templateId={}, templateVersion={}, contextHash={}, model={}, status={}, latencyMs={}",
                  request.requestId(), safeType, scope, templateId, templateVersion, contextHash,
                  properties.openrouter().model(), status, Duration.between(started, Instant.now()).toMillis());
        }
    }

    private void validateRequest(GenerateAiReqDTO request) {
        if (request == null || request.requestId() == null || request.requestId().isBlank()
                || request.type() == null || request.type().isBlank() || request.input() == null
                || request.requestId().length() > 64 || request.type().length() > 64) {
            throw new AiException(400, "AI_INVALID_INPUT", "Input generate AI tidak valid.");
        }
        try { UUID.fromString(request.requestId()); } catch (IllegalArgumentException exception) { throw new AiException(400, "AI_INVALID_INPUT", "Request ID tidak valid."); }
    }

    private Duration remaining(Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        if (remaining.isZero() || remaining.isNegative()) throw new AiException(504, "AI_TIMEOUT", "Waktu generate AI habis.");
        return remaining;
    }
}
