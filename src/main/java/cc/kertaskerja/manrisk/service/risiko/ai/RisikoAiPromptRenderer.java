package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RisikoAiPromptRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}]+)}}");
    private static final int MAX_RESOURCE_BYTES = 32_768;
    private static final int MAX_SYSTEM_CHARS = 8_000;
    private static final int MAX_USER_CHARS = 16_000;
    private final ObjectMapper objectMapper;
    private final String system;
    private final Map<RisikoAiPromptTemplate, String> tasks;

    @Autowired
    public RisikoAiPromptRenderer(ObjectMapper objectMapper) {
        this(objectMapper, RisikoAiPromptRenderer::readClasspathResource);
    }

    RisikoAiPromptRenderer(ObjectMapper objectMapper, Function<String, String> resourceLoader) {
        this.objectMapper = objectMapper;
        this.system = requireResource(resourceLoader.apply("prompts/risiko/v1/system.md"));
        if (system.contains("{{") || system.contains("}}") || system.length() > MAX_SYSTEM_CHARS) throw promptFailed();
        this.tasks = new EnumMap<>(RisikoAiPromptTemplate.class);
        for (RisikoAiPromptTemplate template : RisikoAiPromptTemplate.values()) {
            String task = requireResource(resourceLoader.apply(template.resourcePath()));
            validatePlaceholders(task, template);
            tasks.put(template, task);
        }
    }

    String system() { return system; }

    String render(RisikoAiPromptTemplate template, JsonNode context, Map<String, String> input) {
        String task = tasks.get(template);
        Matcher matcher = PLACEHOLDER.matcher(task);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            JsonNode value = token.startsWith("context.")
                  ? context.get(token.substring("context.".length()))
                  : objectMapper.valueToTree(input.get(token.substring("input.".length())));
            try {
                matcher.appendReplacement(result, Matcher.quoteReplacement(objectMapper.writeValueAsString(value)));
            } catch (JsonProcessingException exception) {
                throw promptFailed();
            }
        }
        matcher.appendTail(result);
        if (result.length() > MAX_USER_CHARS) throw promptFailed();
        return result.toString();
    }

    private void validatePlaceholders(String task, RisikoAiPromptTemplate template) {
        Set<String> expected = new HashSet<>();
        template.contextFields().forEach(field -> expected.add("context." + field));
        template.inputFields().forEach(field -> expected.add("input." + field));
        Set<String> actual = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(task);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!expected.contains(token)) throw promptFailed();
            actual.add(token);
        }
        String unrecognized = matcher.replaceAll("");
        if (task.length() > MAX_USER_CHARS || unrecognized.contains("{{") || unrecognized.contains("}}")
              || !actual.equals(expected)) throw promptFailed();
    }

    private String requireResource(String value) {
        if (value == null || value.isBlank()) throw promptFailed();
        return value.strip();
    }

    private static String readClasspathResource(String path) {
        try (InputStream stream = RisikoAiPromptRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw promptFailed();
            byte[] bytes = stream.readNBytes(MAX_RESOURCE_BYTES + 1);
            if (bytes.length > MAX_RESOURCE_BYTES) throw promptFailed();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw promptFailed();
        }
    }

    private static AiException promptFailed() {
        return new AiException(500, "AI_PROMPT_FAILED", "Prompt AI tidak dapat dibuat.");
    }
}
