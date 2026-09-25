package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        RisikoAiPromptFactory.Prompt prompt = new RisikoAiPromptFactory(objectMapper).build(request, context);

        assertTrue(prompt.system().contains("kinerja operasional individu"));
    }

    @Test
    void buildsSchemasForControlAndRealizationDrafts() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode context = objectMapper.createObjectNode().put("scope", "pemda");
        RisikoAiPromptFactory factory = new RisikoAiPromptFactory(objectMapper);

        RisikoAiPromptFactory.Prompt control = factory.build(request("pengendalian-yang-sudah-ada",
              Map.of("pernyataan_risiko", "Pelayanan terganggu")), context);
        RisikoAiPromptFactory.Prompt realization = factory.build(request("realisasi-tindak-pengendalian",
              Map.of("rencana_tindak_pengendalian", "Melakukan monitoring")), context);

        assertEquals(3, control.schema().path("properties").path("proposals").path("minItems").asInt());
        assertTrue(control.user().contains("diverifikasi pengguna"));
        assertEquals(3, realization.schema().path("properties").path("proposals").path("maxItems").asInt());
        assertTrue(realization.user().contains("tidak boleh mengarang bukti pelaksanaan"));
    }

    @Test
    void rejectsMissingOrUnexpectedInputForNewAiTypes() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode context = objectMapper.createObjectNode().put("scope", "opd");
        RisikoAiPromptFactory factory = new RisikoAiPromptFactory(objectMapper);

        assertThrows(cc.kertaskerja.manrisk.exception.AiException.class,
              () -> factory.build(request("pengendalian-yang-sudah-ada", Map.of()), context));
        assertThrows(cc.kertaskerja.manrisk.exception.AiException.class,
              () -> factory.build(request("realisasi-tindak-pengendalian", Map.of("pernyataan_risiko", "Tidak diizinkan")), context));
    }

    private GenerateAiReqDTO request(String type, Map<String, String> input) {
        return new GenerateAiReqDTO(UUID.randomUUID().toString(), type, null, input);
    }
}
