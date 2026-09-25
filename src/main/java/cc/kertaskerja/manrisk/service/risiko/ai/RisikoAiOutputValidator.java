package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RisikoAiOutputValidator {
    private final ObjectMapper objectMapper;

    public JsonNode normalize(String type, JsonNode raw) {
        if (raw == null || !raw.isObject()) invalid("root output bukan object");
        return switch (type) {
            case "permasalahan" -> pair(raw, "permasalahan", "sebab_permasalahan");
            case "dampak" -> single(raw, "dampak");
            case "pernyataan-risiko" -> proposals(raw, 4, "pernyataan_risiko", this::pernyataan);
            case "rtp" -> proposals(raw, 3, "rencana_tindak_pengendalian", this::rtp);
            case "metode-pemantauan" -> proposals(raw, 5, "aktivitas_pemantauan", this::metode);
            case "pengendalian-yang-sudah-ada" -> proposals(raw, 3, "pengendalian_yang_sudah_ada", this::pengendalianYangSudahAda);
            case "realisasi-tindak-pengendalian" -> proposals(raw, 3, "realisasi_tindak_pengendalian", this::realisasiTindakPengendalian);
            default -> throw new AiException(400, "AI_TYPE_INVALID", "Tipe generate AI tidak dikenali.");
        };
    }

    private ObjectNode pair(JsonNode raw, String first, String second) {
        ObjectNode result = objectMapper.createObjectNode(); result.put(first, text(raw, first, 1500)); result.put(second, text(raw, second, 1500)); return result;
    }
    private ObjectNode single(JsonNode raw, String name) {
        ObjectNode result = objectMapper.createObjectNode(); result.put(name, text(raw, name, 1500)); return result;
    }
    private ObjectNode proposals(JsonNode raw, int count, String uniqueField, ProposalNormalizer normalizer) {
        JsonNode candidates = raw.path("proposals");
        if (!candidates.isArray() || candidates.size() != count) {
            invalid("jumlah proposals tidak sesuai; expected=" + count + ", actual=" + (candidates.isArray() ? candidates.size() : "non-array"));
        }
        ObjectNode result = objectMapper.createObjectNode(); ArrayNode proposals = result.putArray("proposals"); Set<String> seen = new HashSet<>();
        for (JsonNode candidate : candidates) {
            ObjectNode proposal = normalizer.normalize(candidate);
            String uniqueValue = proposal.path(uniqueField).asText().toLowerCase(java.util.Locale.ROOT);
            if (!seen.add(uniqueValue)) invalid("proposal duplikat pada field " + uniqueField);
            proposal.put("id", UUID.randomUUID().toString());
            proposals.add(proposal);
        }
        return result;
    }
    private ObjectNode pernyataan(JsonNode raw) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("kategori", text(raw, "kategori", 100)); result.put("pernyataan_risiko", text(raw, "pernyataan_risiko", 1500));
        result.put("peristiwa", text(raw, "peristiwa", 1500)); result.put("penyebab", text(raw, "penyebab", 1500)); result.put("dampak", text(raw, "dampak", 1500)); return result;
    }
    private ObjectNode rtp(JsonNode raw) {
        ObjectNode result = objectMapper.createObjectNode(); result.put("pendekatan", text(raw, "pendekatan", 1500));
        result.put("rencana_tindak_pengendalian", text(raw, "rencana_tindak_pengendalian", 1500));
        ObjectNode rtp = result.putObject("rtp"); rtp.set("preventif", texts(raw.path("rtp"), "preventif")); rtp.set("detektif", texts(raw.path("rtp"), "detektif")); rtp.set("korektif", texts(raw.path("rtp"), "korektif")); return result;
    }
    private ObjectNode metode(JsonNode raw) {
        ObjectNode result = objectMapper.createObjectNode(); result.put("aktivitas_pemantauan", text(raw, "aktivitas_pemantauan", 1500));
        String sifat = text(raw, "sifat", 30); if (!Set.of("Berkala", "Berkelanjutan").contains(sifat)) invalid("nilai sifat tidak dikenali");
        result.put("sifat", sifat); result.put("frekuensi", text(raw, "frekuensi", 100)); return result;
    }
    private ObjectNode pengendalianYangSudahAda(JsonNode raw) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("pengendalian_yang_sudah_ada", text(raw, "pengendalian_yang_sudah_ada", 1500));
        return result;
    }
    private ObjectNode realisasiTindakPengendalian(JsonNode raw) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("realisasi_tindak_pengendalian", text(raw, "realisasi_tindak_pengendalian", 1500));
        return result;
    }
    private ArrayNode texts(JsonNode parent, String field) {
        JsonNode raw = parent.path(field);
        if (!raw.isArray() || raw.isEmpty() || raw.size() > 3) invalid("field " + field + " harus array berisi 1-3 item");
        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode item : raw) {
            if (!item.isTextual() || item.asText().trim().isEmpty() || item.asText().trim().length() > 500) {
                invalid("item field " + field + " kosong, bukan teks, atau terlalu panjang");
            }
            result.add(item.asText().trim());
        }
        return result;
    }
    private String text(JsonNode parent, String field, int maxLength) {
        JsonNode raw = parent.path(field);
        if (!raw.isTextual() || raw.asText().trim().isEmpty() || raw.asText().trim().length() > maxLength) {
            invalid("field " + field + " kosong, bukan teks, atau melebihi " + maxLength + " karakter");
        }
        return raw.asText().trim();
    }
    private void invalid(String reason) {
        log.warn("AI output ditolak validator: {}", reason);
        throw new AiException(502, "AI_INVALID_OUTPUT", "AI tidak mengembalikan usulan yang valid.");
    }
    @FunctionalInterface private interface ProposalNormalizer { ObjectNode normalize(JsonNode raw); }
}
