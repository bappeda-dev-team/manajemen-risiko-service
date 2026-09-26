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
        validateConfiguration();

        Instant startedAt = Instant.now();
        Instant deadline = createDeadline();
        GenerationLog generationLog = new GenerationLog();

        try (RisikoAiRequestGuard.Permit ignored = requestGuard.acquire(caller, request.requestId())) {
            RisikoAiContextService.ResolvedContext context = contextService.normalize(request.context());

            generationLog.setContext(
                    request.type(),
                    context.value().path("scope").asText("unknown"),
                    context.hash()
            );

            RisikoAiPromptFactory.Prompt prompt = promptFactory.build(request, context.value());

            generationLog.setPrompt(prompt.templateId(), prompt.templateVersion());

            Duration providerTimeout = calculateProviderTimeout(deadline);

            JsonNode providerOutput = openRouterClient.generate(prompt, providerTimeout);

            JsonNode normalizedOutput = outputValidator.normalize(request.type(), providerOutput);

            return createResponse(request, context, normalizedOutput);
        } catch (AiException exception) {
            generationLog.setStatus(exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            generationLog.setStatus("AI_INTERNAL_ERROR");
            throw exception;
        } finally {
            logGeneration(request, generationLog, startedAt);
        }
    }

    private void validateRequest(GenerateAiReqDTO request) {
        if (
                request == null
                        || request.requestId() == null
                        || request.requestId().isBlank()
                        || request.type() == null
                        || request.type().isBlank()
                        || request.input() == null
                        || request.requestId().length() > 64
                        || request.type().length() > 64
        ) {
            throw new AiException(400, "AI_INVALID_INPUT", "Input generate AI tidak valid.");
        }

        validateRequestId(request.requestId());
    }

    private void validateRequestId(String requestId) {
        try {
            UUID.fromString(requestId);
        } catch (IllegalArgumentException exception) {
            throw new AiException(400, "AI_INVALID_INPUT", "Request ID tidak valid.");
        }
    }

    private void validateConfiguration() {
        if (!properties.enabled()) {
            throw new AiException(503, "AI_DISABLED", "Fitur generate AI belum diaktifkan.");
        }

        if (!properties.isConfigured()) {
            throw new AiException(503, "AI_NOT_CONFIGURED", "Konfigurasi layanan AI belum lengkap.");
        }
    }

    private Instant createDeadline() {
        int requestTimeoutSeconds = Math.max(1, properties.requestTimeoutSeconds());

        return Instant.now().plusSeconds(requestTimeoutSeconds);
    }

    private Duration calculateProviderTimeout(Instant deadline) {
        Duration remaining = calculateRemainingTime(deadline);

        long configuredTimeout = Math.max(1, properties.providerTimeoutSeconds());
        long remainingSeconds = Math.max(1, remaining.toSeconds());
        long providerTimeoutSeconds = Math.min(configuredTimeout, remainingSeconds);

        return Duration.ofSeconds(providerTimeoutSeconds);
    }

    private Duration calculateRemainingTime(Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);

        if (remaining.isZero() || remaining.isNegative()) {
            throw new AiException(504, "AI_TIMEOUT", "Waktu generate AI habis.");
        }

        return remaining;
    }

    private GenerateAiResDTO createResponse(
            GenerateAiReqDTO request,
            RisikoAiContextService.ResolvedContext context,
            JsonNode normalizedOutput
    ) {
        /*
         * Jangan mengekspos JsonNode langsung melalui DTO.
         * Serializer selain Jackson dapat menampilkan metadata
         * Java bean, bukan payload JSON yang dihasilkan.
         */
        Object result = objectMapper.convertValue(normalizedOutput, Object.class);

        return new GenerateAiResDTO(
                request.requestId(),
                request.type(),
                context.hash(),
                result,
                properties.openrouter().model()
        );
    }

    private void logGeneration(GenerateAiReqDTO request, GenerationLog generationLog, Instant startedAt) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

        log.info(
                "AI generation: requestId={}, type={}, scope={}, templateId={}, "
                        + "templateVersion={}, contextHash={}, model={}, status={}, latencyMs={}",
                request.requestId(),
                generationLog.type,
                generationLog.scope,
                generationLog.templateId,
                generationLog.templateVersion,
                generationLog.contextHash,
                properties.openrouter().model(),
                generationLog.status,
                latencyMs
        );
    }

    private static class GenerationLog {
        private String type = "unknown";
        private String scope = "unknown";
        private String templateId = "unresolved";
        private String templateVersion = "unresolved";
        private String contextHash = "unresolved";
        private String status = "success";

        private void setContext(String type, String scope, String contextHash) {
            this.type = type;
            this.scope = scope;
            this.contextHash = contextHash;
        }

        private void setPrompt(String templateId, String templateVersion) {
            this.templateId = templateId;
            this.templateVersion = templateVersion;
        }

        private void setStatus(String status) {
            this.status = status;
        }
    }
}