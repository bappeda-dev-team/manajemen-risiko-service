package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiResDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
        if (!properties.enabled()) throw new AiException(503, "AI_DISABLED", "Fitur generate AI belum diaktifkan.");
        if (!properties.isConfigured()) throw new AiException(503, "AI_NOT_CONFIGURED", "Konfigurasi layanan AI belum lengkap.");

        Instant deadline = Instant.now().plusSeconds(Math.max(1, properties.requestTimeoutSeconds()));
        try (RisikoAiRequestGuard.Permit ignored = requestGuard.acquire(caller, request.requestId())) {
            RisikoAiContextService.ResolvedContext context = contextService.normalize(request.context());
            Duration remaining = remaining(deadline);
            RisikoAiPromptFactory.Prompt prompt = promptFactory.build(request, context.value());
            JsonNode output = openRouterClient.generate(prompt,
                    Duration.ofSeconds(Math.min(Math.max(1, properties.providerTimeoutSeconds()), Math.max(1, remaining.toSeconds()))));
            JsonNode normalized = outputValidator.normalize(request.type(), output);
            // Do not expose JsonNode in an API DTO: non-Jackson serializers render its
            // Java bean metadata instead of the generated JSON payload.
            Object result = objectMapper.convertValue(normalized, Object.class);
            return new GenerateAiResDTO(request.requestId(), request.type(), context.hash(), result,
                    properties.openrouter().model());
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
