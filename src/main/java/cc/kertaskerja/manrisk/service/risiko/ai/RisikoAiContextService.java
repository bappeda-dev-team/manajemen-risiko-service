package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import cc.kertaskerja.manrisk.service.risiko.external.ExternalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class RisikoAiContextService {
    private final ExternalService externalService;
    private final ObjectMapper objectMapper;

    public ResolvedContext resolve(GenerateAiReqDTO.Scope scope) {
        JsonNode root;
        try {
            root = externalService.getTujuanSasaran(scope.kodeOpd(), scope.tahun());
        } catch (RuntimeException exception) {
            throw new AiException(502, "AI_CONTEXT_UNAVAILABLE", "Konteks sasaran belum dapat dimuat.", exception);
        }

        JsonNode data = root == null ? null : root.path("data");
        if (data == null || data.isMissingNode()
                || !scope.kodeOpd().equals(data.path("kode_opd").asText())) {
            throw new AiException(404, "AI_SASARAN_NOT_FOUND", "Sasaran OPD tidak ditemukan.");
        }

        JsonNode foundTujuan = null;
        JsonNode foundSasaran = null;
        for (JsonNode tujuan : data.path("tujuan_opds")) {
            for (JsonNode sasaran : tujuan.path("sasaran_opds")) {
                if (scope.kodeSasaran().equals(sasaran.path("kode_sasaran_opd").asText())) {
                    foundTujuan = tujuan;
                    foundSasaran = sasaran;
                    break;
                }
            }
            if (foundSasaran != null) break;
        }

        if (foundSasaran == null) {
            throw new AiException(404, "AI_SASARAN_NOT_FOUND", "Sasaran OPD tidak ditemukan.");
        }

        JsonNode indicator = selectIndicator(foundSasaran.path("indikators"), scope.kodeIndikator());
        ObjectNode context = objectMapper.createObjectNode();
        context.put("kode_opd", scope.kodeOpd());
        context.put("tahun", scope.tahun());
        putTextOrNull(context, "kode_tujuan_opd", foundTujuan.path("kode_tujuan_opd"));
        putTextOrNull(context, "tujuan_opd", foundTujuan.path("tujuan_opd"));
        context.put("kode_sasaran_opd", scope.kodeSasaran());
        putTextOrNull(context, "sasaran_opd", foundSasaran.path("sasaran_opd"));
        putTextOrNull(context, "kode_indikator", indicator == null ? null : indicator.path("kode_indikator"));
        putTextOrNull(context, "indikator", indicator == null ? null : indicator.path("indikator"));
        JsonNode target = selectTarget(indicator == null ? null : indicator.path("targets"), scope.tahun());
        if (target == null) {
            context.putNull("target");
            context.putNull("satuan");
        } else {
            if (target.path("target").isNumber()) context.set("target", target.path("target"));
            else context.putNull("target");
            putTextOrNull(context, "satuan", target.path("satuan"));
        }
        // API Penetapan saat ini tidak menyediakan pagu/pemilik risiko yang dapat diverifikasi.
        context.putNull("pagu");
        context.putNull("pemilik_risiko");

        return new ResolvedContext(context, sha256(context));
    }

    private JsonNode selectIndicator(JsonNode indicators, String kodeIndikator) {
        if (!indicators.isArray() || indicators.isEmpty()) return null;
        if (kodeIndikator == null || kodeIndikator.isBlank()) {
            if (indicators.size() == 1) return indicators.get(0);
            throw new AiException(400, "AI_INDICATOR_REQUIRED", "Pilih indikator sebelum generate rekomendasi.");
        }
        for (JsonNode indicator : indicators) {
            if (kodeIndikator.equals(indicator.path("kode_indikator").asText())) return indicator;
        }
        throw new AiException(400, "AI_INDICATOR_INVALID", "Indikator tidak sesuai dengan sasaran.");
    }

    private JsonNode selectTarget(JsonNode targets, int tahun) {
        if (targets == null || !targets.isArray()) return null;
        for (JsonNode target : targets) {
            if (target.path("tahun").asInt(Integer.MIN_VALUE) == tahun) return target;
        }
        return null;
    }

    private static void putTextOrNull(ObjectNode target, String field, JsonNode source) {
        if (source != null && source.isTextual() && !source.asText().isBlank()) target.put(field, source.asText().trim());
        else target.putNull(field);
    }

    private String sha256(ObjectNode context) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(context).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new AiException(500, "AI_CONTEXT_FINGERPRINT_FAILED", "Konteks AI tidak dapat divalidasi.", exception);
        }
    }

    public record ResolvedContext(ObjectNode value, String fingerprint) {}
}
