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

    private static final int PROPOSAL_COUNT = 4;

    private final ObjectMapper objectMapper;
    private final RisikoAiPromptRenderer renderer;

    public Prompt build(GenerateAiReqDTO request, JsonNode context) {
        RisikoAiPromptTemplate template = RisikoAiPromptTemplate.fromType(request.type());

        validateInput(template, request.input());

        String systemPrompt = renderer.system();
        String userPrompt = renderer.render(template, context, request.input());
        JsonNode schema = responseSchema(template.type());

        return new Prompt(template.templateId(), template.version(), systemPrompt, userPrompt, schema);
    }

    private void validateInput(RisikoAiPromptTemplate template, Map<String, String> input) {
        if (input == null) {
            throw new AiException(400, "AI_INVALID_INPUT", "Input generate AI tidak valid.");
        }

        boolean hasUnknownField = !template.inputFields().containsAll(input.keySet());

        boolean hasInvalidValue = input.values()
                .stream()
                .anyMatch(value -> value == null || value.trim().length() > 2000);

        if (hasUnknownField || hasInvalidValue) {
            throw new AiException(400, "AI_INVALID_INPUT", "Input generate AI tidak valid.");
        }

        boolean hasMissingRequiredInput = template.requiredInput()
                .stream()
                .anyMatch(field -> input.get(field) == null || input.get(field).isBlank());

        if (hasMissingRequiredInput) {
            throw new AiException(400, "AI_INPUT_REQUIRED", "Lengkapi input yang diperlukan sebelum generate AI.");
        }
    }

    private JsonNode responseSchema(String type) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");

        switch (type) {
            case "permasalahan" -> {
                addStringProperty(properties, "permasalahan");
                addStringProperty(properties, "sebab_permasalahan");

                addRequiredFields(schema, "permasalahan", "sebab_permasalahan");
            }

            case "dampak" -> {
                addStringProperty(properties, "dampak");
                addRequiredFields(schema, "dampak");
            }

            default -> {
                ObjectNode proposals = properties.putObject("proposals");

                proposals.put("type", "array");
                proposals.put("minItems", PROPOSAL_COUNT);
                proposals.put("maxItems", PROPOSAL_COUNT);
                proposals.set("items", proposalSchema(type));

                addRequiredFields(schema, "proposals");
            }
        }

        return schema;
    }

    private ObjectNode proposalSchema(String type) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);

        ObjectNode properties = schema.putObject("properties");

        switch (type) {
            case "pernyataan-risiko" -> {
                addStringProperty(properties, "kategori", 100);
                addStringProperty(properties, "pernyataan_risiko");
                addStringProperty(properties, "peristiwa");
                addStringProperty(properties, "penyebab");
                addStringProperty(properties, "dampak");

                addRequiredFields(
                        schema,
                        "kategori",
                        "pernyataan_risiko",
                        "peristiwa",
                        "penyebab",
                        "dampak"
                );
            }

            case "rtp" -> {
                addStringProperty(properties, "pendekatan");
                addStringProperty(properties, "rencana_tindak_pengendalian");

                ObjectNode rtp = properties.putObject("rtp");
                rtp.put("type", "object");
                rtp.put("additionalProperties", false);

                ObjectNode rtpProperties = rtp.putObject("properties");

                addStringArrayProperty(rtpProperties, "preventif");
                addStringArrayProperty(rtpProperties, "detektif");
                addStringArrayProperty(rtpProperties, "korektif");

                addRequiredFields(rtp, "preventif", "detektif", "korektif");
                addRequiredFields(schema, "pendekatan", "rencana_tindak_pengendalian", "rtp");
            }

            case "pengendalian-yang-sudah-ada" -> {
                addStringProperty(properties, "pengendalian_yang_sudah_ada");
                addRequiredFields(schema, "pengendalian_yang_sudah_ada");
            }

            case "realisasi-tindak-pengendalian" -> {
                addStringProperty(properties, "realisasi_tindak_pengendalian");
                addRequiredFields(schema, "realisasi_tindak_pengendalian");
            }

            case "metode-pemantauan" -> {
                addStringProperty(properties, "aktivitas_pemantauan");
                addEnumProperty(properties, "sifat", "Berkala", "Berkelanjutan");
                addStringProperty(properties, "frekuensi", 100);

                addRequiredFields(schema, "aktivitas_pemantauan", "sifat", "frekuensi");
            }

            default -> throw new IllegalArgumentException("Tipe proposal tidak dikenali: " + type);
        }

        return schema;
    }

    private void addStringProperty(ObjectNode properties, String name) {
        addStringProperty(properties, name, 1500);
    }

    private void addStringProperty(ObjectNode properties, String name, int maxLength) {
        ObjectNode property = properties.putObject(name);

        property.put("type", "string");
        property.put("minLength", 1);
        property.put("maxLength", maxLength);
    }

    private void addEnumProperty(ObjectNode properties, String name, String... values) {
        ObjectNode property = properties.putObject(name);
        property.put("type", "string");

        var allowedValues = property.putArray("enum");

        for (String value : values) {
            allowedValues.add(value);
        }
    }

    private void addStringArrayProperty(ObjectNode properties, String name) {
        ObjectNode property = properties.putObject(name);

        property.put("type", "array");
        property.put("minItems", 1);
        property.put("maxItems", 3);

        ObjectNode item = property.putObject("items");
        item.put("type", "string");
        item.put("minLength", 1);
        item.put("maxLength", 500);
    }

    private void addRequiredFields(ObjectNode schema, String... fields) {
        var requiredFields = schema.putArray("required");

        for (String field : fields) {
            requiredFields.add(field);
        }
    }

    public record Prompt(String templateId, String templateVersion, String system, String user, JsonNode schema) {
    }
}