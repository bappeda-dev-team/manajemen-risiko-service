package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RisikoAiPromptFactoryTest {
    @Test
    void usesOperationalSubjectForOperationalScope() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode context = objectMapper.createObjectNode().put("scope", "operasional");
        GenerateAiReqDTO request = new GenerateAiReqDTO(
              UUID.randomUUID().toString(), "permasalahan", null,
              Map.of("permasalahan", "", "sebab_permasalahan", ""));

        RisikoAiPromptFactory.Prompt prompt = factory(objectMapper).build(request, context);

        assertTrue(prompt.system().contains("kinerja operasional individu"));
        assertTrue(prompt.user().contains("\"scope\": \"operasional\""));
        assertEquals("risiko/permasalahan", prompt.templateId());
        assertEquals("v1", prompt.templateVersion());
    }

    @Test
    void buildsSchemasForControlAndRealizationDrafts() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode context = objectMapper.createObjectNode().put("scope", "pemda");
        RisikoAiPromptFactory factory = factory(objectMapper);

        RisikoAiPromptFactory.Prompt control = factory.build(request("pengendalian-yang-sudah-ada",
              Map.of("pernyataan_risiko", "Pelayanan terganggu")), context);
        RisikoAiPromptFactory.Prompt realization = factory.build(request("realisasi-tindak-pengendalian",
              Map.of("rencana_tindak_pengendalian", "Melakukan monitoring")), context);

        assertEquals(4, control.schema().path("properties").path("proposals").path("minItems").asInt());
        assertTrue(control.user().contains("diverifikasi pengguna"));
        assertEquals(4, realization.schema().path("properties").path("proposals").path("maxItems").asInt());
        assertTrue(realization.user().contains("tidak boleh mengarang bukti pelaksanaan"));
    }

    @Test
    void rejectsMissingOrUnexpectedInputForNewAiTypes() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode context = objectMapper.createObjectNode().put("scope", "opd");
        RisikoAiPromptFactory factory = factory(objectMapper);

        assertThrows(cc.kertaskerja.manrisk.exception.AiException.class,
              () -> factory.build(request("pengendalian-yang-sudah-ada", Map.of()), context));
        assertThrows(cc.kertaskerja.manrisk.exception.AiException.class,
              () -> factory.build(request("realisasi-tindak-pengendalian", Map.of("pernyataan_risiko", "Tidak diizinkan")), context));
    }

    @Test
    void preservesRequiredInputsAndProviderSchemasForAllTypes() {
        ObjectMapper mapper = new ObjectMapper();
        RisikoAiPromptFactory factory = factory(mapper);
        ObjectNode context = mapper.createObjectNode().put("scope", "pemda");

        assertEquals("string", factory.build(request("permasalahan", Map.of()), context)
              .schema().path("properties").path("permasalahan").path("type").asText());
        assertEquals("object", factory.build(request("dampak", Map.of("pernyataan_risiko", "Risiko")), context)
              .schema().path("type").asText());
        assertProposalCount(factory, context, "pernyataan-risiko", Map.of("permasalahan", "Masalah", "sebab_permasalahan", "Sebab"), 4);
        assertProposalCount(factory, context, "rtp", Map.of("pernyataan_risiko", "Risiko"), 4);
        assertProposalCount(factory, context, "metode-pemantauan", Map.of("pernyataan_risiko", "Risiko", "rencana_tindak_pengendalian", "RTP"), 4);
        assertProposalCount(factory, context, "pengendalian-yang-sudah-ada", Map.of("pernyataan_risiko", "Risiko"), 4);
        assertProposalCount(factory, context, "realisasi-tindak-pengendalian", Map.of("rencana_tindak_pengendalian", "RTP"), 4);

        AiException missing = assertThrows(AiException.class,
              () -> factory.build(request("pernyataan-risiko", Map.of("permasalahan", "Masalah")), context));
        assertEquals("AI_INPUT_REQUIRED", missing.getCode());
        AiException unexpected = assertThrows(AiException.class,
              () -> factory.build(request("realisasi-tindak-pengendalian", Map.of("rencana_tindak_pengendalian", "RTP", "tanggal", "2026")), context));
        assertEquals("AI_INVALID_INPUT", unexpected.getCode());
        AiException oversized = assertThrows(AiException.class,
              () -> factory.build(request("pengendalian-yang-sudah-ada", Map.of("pernyataan_risiko", "x".repeat(2001))), context));
        assertEquals("AI_INVALID_INPUT", oversized.getCode());
    }

    private void assertProposalCount(RisikoAiPromptFactory factory, ObjectNode context,
                                     String type, Map<String, String> input, int count) {
        RisikoAiPromptFactory.Prompt prompt = factory.build(request(type, input), context);
        assertEquals(count, prompt.schema().path("properties").path("proposals").path("minItems").asInt());
        assertEquals(count, prompt.schema().path("properties").path("proposals").path("maxItems").asInt());
        assertFalse(prompt.user().contains("{{"));
    }

    private GenerateAiReqDTO request(String type, Map<String, String> input) {
        return new GenerateAiReqDTO(UUID.randomUUID().toString(), type, null, input);
    }

    private RisikoAiPromptFactory factory(ObjectMapper mapper) {
        return new RisikoAiPromptFactory(mapper, new RisikoAiPromptRenderer(mapper));
    }
}
