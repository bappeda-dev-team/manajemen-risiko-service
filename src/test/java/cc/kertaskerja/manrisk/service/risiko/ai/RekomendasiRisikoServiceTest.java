package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiResDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RekomendasiRisikoServiceTest {
    @Mock private RisikoAiContextService contextService;
    @Mock private RisikoAiPromptFactory promptFactory;
    @Mock private OpenRouterClient openRouterClient;
    @Mock private RisikoAiOutputValidator outputValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RekomendasiRisikoService service;

    @BeforeEach
    void setUp() {
        RisikoAiProperties properties = new RisikoAiProperties(
                true,
                new RisikoAiProperties.OpenRouter("https://openrouter.ai/api/v1", "test-key", "test-model"),
                30, 4, 5, 60, 30);
        service = new RekomendasiRisikoService(
                properties,
                new RisikoAiRequestGuard(properties),
                contextService,
                promptFactory,
                openRouterClient,
                outputValidator,
                objectMapper);
    }

    @Test
    void usesClientContextAndReturnsServerContextHash() {
        GenerateAiReqDTO.Context clientContext = new GenerateAiReqDTO.Context(
                null, "OPD-001", 2026, null, null, "SAS-001", "Sasaran utama",
                null, null, null, null, null, null, null, null, null, "OPD Contoh");
        GenerateAiReqDTO request = new GenerateAiReqDTO(
                UUID.randomUUID().toString(), "permasalahan", clientContext,
                Map.of("permasalahan", "", "sebab_permasalahan", ""));
        ObjectNode canonicalContext = objectMapper.createObjectNode().put("kode_opd", "OPD-001");
        RisikoAiContextService.ResolvedContext resolved =
                new RisikoAiContextService.ResolvedContext(canonicalContext, "a".repeat(64));
        RisikoAiPromptFactory.Prompt prompt =
                new RisikoAiPromptFactory.Prompt("risiko/permasalahan", "v1", "system", "user", objectMapper.createObjectNode());
        ObjectNode providerOutput = objectMapper.createObjectNode().put("permasalahan", "Masalah");

        when(contextService.normalize(clientContext)).thenReturn(resolved);
        when(promptFactory.build(request, canonicalContext)).thenReturn(prompt);
        when(openRouterClient.generate(eq(prompt), any(Duration.class))).thenReturn(providerOutput);
        when(outputValidator.normalize("permasalahan", providerOutput)).thenReturn(providerOutput);

        GenerateAiResDTO response = service.generate("user-1", request);

        assertEquals("a".repeat(64), response.contextHash());
        assertEquals("test-model", response.model());
        verify(contextService).normalize(clientContext);
    }
}
