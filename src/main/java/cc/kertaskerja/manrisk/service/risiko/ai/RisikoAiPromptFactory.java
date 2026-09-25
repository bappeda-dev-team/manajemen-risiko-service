package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RisikoAiPromptFactory {
    private final ObjectMapper objectMapper;
    private final RisikoAiPromptRenderer renderer;

    public Prompt build(GenerateAiReqDTO request, JsonNode context) {
        RisikoAiPromptTemplate template = RisikoAiPromptTemplate.fromType(request.type());
        validateInput(template, request.input());
        return new Prompt(template.templateId(), template.version(), renderer.system(),
              renderer.render(template, context, request.input()), responseSchema(template.type()));
    }

    private void validateInput(RisikoAiPromptTemplate template, Map<String, String> input) {
        if (input == null || !template.inputFields().containsAll(input.keySet())
              || input.values().stream().anyMatch(value -> value == null || value.trim().length() > 2000)) {
            throw new AiException(400, "AI_INVALID_INPUT", "Input generate AI tidak valid.");
        }
        if (template.requiredInput().stream().anyMatch(field -> input.get(field) == null || input.get(field).isBlank())) {
            throw new AiException(400, "AI_INPUT_REQUIRED", "Lengkapi input yang diperlukan sebelum generate AI.");
        }
    }

    private JsonNode responseSchema(String type) {
        // The provider schema keeps top-level output constrained; detailed validation still happens locally.
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        if (type.equals("permasalahan")) {
            text(properties, "permasalahan"); text(properties, "sebab_permasalahan");
            required(schema, "permasalahan", "sebab_permasalahan");
        } else if (type.equals("dampak")) {
            text(properties, "dampak"); required(schema, "dampak");
        } else {
            ObjectNode proposals = properties.putObject("proposals");
            proposals.put("type", "array");
            proposals.put("minItems", proposalCount(type));
            proposals.put("maxItems", proposalCount(type));
            proposals.set("items", proposalSchema(type));
            required(schema, "proposals");
        }
        return schema;
    }

    private ObjectNode proposalSchema(String type) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object"); schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        if (type.equals("pernyataan-risiko")) {
            text(properties, "kategori", 100); text(properties, "pernyataan_risiko"); text(properties, "peristiwa"); text(properties, "penyebab"); text(properties, "dampak");
            required(schema, "kategori", "pernyataan_risiko", "peristiwa", "penyebab", "dampak");
        } else if (type.equals("rtp")) {
            text(properties, "pendekatan"); text(properties, "rencana_tindak_pengendalian");
            ObjectNode rtp = properties.putObject("rtp"); rtp.put("type", "object"); rtp.put("additionalProperties", false);
            ObjectNode rtpProperties = rtp.putObject("properties"); stringArray(rtpProperties, "preventif"); stringArray(rtpProperties, "detektif"); stringArray(rtpProperties, "korektif");
            required(rtp, "preventif", "detektif", "korektif"); required(schema, "pendekatan", "rencana_tindak_pengendalian", "rtp");
        } else if (type.equals("pengendalian-yang-sudah-ada")) {
            text(properties, "pengendalian_yang_sudah_ada");
            required(schema, "pengendalian_yang_sudah_ada");
        } else if (type.equals("realisasi-tindak-pengendalian")) {
            text(properties, "realisasi_tindak_pengendalian");
            required(schema, "realisasi_tindak_pengendalian");
        } else {
            text(properties, "aktivitas_pemantauan"); enumText(properties, "sifat", "Berkala", "Berkelanjutan"); text(properties, "frekuensi", 100);
            required(schema, "aktivitas_pemantauan", "sifat", "frekuensi");
        }
        return schema;
    }

    private void text(ObjectNode properties, String name) { text(properties, name, 1500); }
    private void text(ObjectNode properties, String name, int maxLength) { ObjectNode item = properties.putObject(name); item.put("type", "string"); item.put("minLength", 1); item.put("maxLength", maxLength); }
    private void enumText(ObjectNode properties, String name, String... values) { ObjectNode item = properties.putObject(name); item.put("type", "string"); var allowed = item.putArray("enum"); for (String value : values) allowed.add(value); }
    private void stringArray(ObjectNode properties, String name) { ObjectNode array = properties.putObject(name); array.put("type", "array"); array.put("minItems", 1); array.put("maxItems", 3); ObjectNode item = array.putObject("items"); item.put("type", "string"); item.put("minLength", 1); item.put("maxLength", 500); }
    private void required(ObjectNode object, String... fields) { var required = object.putArray("required"); for (String field : fields) required.add(field); }
    private int proposalCount(String type) { return switch (type) { case "pernyataan-risiko" -> 4; case "rtp", "pengendalian-yang-sudah-ada", "realisasi-tindak-pengendalian" -> 3; case "metode-pemantauan" -> 5; default -> throw new IllegalArgumentException(); }; }
    public record Prompt(String templateId, String templateVersion, String system, String user, JsonNode schema) {}
}
