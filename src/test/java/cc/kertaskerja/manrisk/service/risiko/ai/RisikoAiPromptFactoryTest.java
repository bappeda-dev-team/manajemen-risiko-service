package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

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
}
