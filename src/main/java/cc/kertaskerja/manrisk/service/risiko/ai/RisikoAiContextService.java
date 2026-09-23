package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RisikoAiContextService {
    private final ObjectMapper objectMapper;

    /** Context frontend adalah snapshot tidak tepercaya yang hanya digunakan untuk prompt AI. */
    public ResolvedContext normalize(GenerateAiReqDTO.Context source) {
        if (source == null) throw contextInvalid();

        String scope = source.scope() == null || source.scope().isBlank()
              ? "opd" : source.scope().trim().toLowerCase(Locale.ROOT);
        if (!("opd".equals(scope) || "pemda".equals(scope))) throw contextInvalid();
        if (source.tahun() == null || source.tahun() < 1900 || source.tahun() > 2100
              || (source.pagu() != null && source.pagu().signum() < 0)) {
            throw contextInvalid();
        }

        boolean hasOpdContext = hasText(source.kodeOpd()) || hasText(source.kodeTujuanOpd())
              || hasText(source.tujuanOpd()) || hasText(source.kodeSasaranOpd()) || hasText(source.sasaranOpd());
        boolean hasPemdaContext = hasText(source.kodeTujuanPemda()) || hasText(source.tujuanPemda())
              || hasText(source.kodeSasaranPemda()) || hasText(source.sasaranPemda());

        String kodeOpd = null;
        String kodeTujuan;
        String tujuan;
        String kodeSasaran;
        String sasaran;
        if ("pemda".equals(scope)) {
            if (hasOpdContext) throw contextInvalid();
            kodeTujuan = optionalText(source.kodeTujuanPemda(), 128);
            tujuan = optionalText(source.tujuanPemda(), 2000);
            kodeSasaran = requiredText(source.kodeSasaranPemda(), 128);
            sasaran = requiredText(source.sasaranPemda(), 2000);
        } else {
            if (hasPemdaContext) throw contextInvalid();
            kodeOpd = requiredText(source.kodeOpd(), 128);
            kodeTujuan = optionalText(source.kodeTujuanOpd(), 128);
            tujuan = optionalText(source.tujuanOpd(), 2000);
            kodeSasaran = requiredText(source.kodeSasaranOpd(), 128);
            sasaran = requiredText(source.sasaranOpd(), 2000);
        }

        ObjectNode context = objectMapper.createObjectNode();
        context.put("scope", scope);
        putTextOrNull(context, "kode_opd", kodeOpd);
        context.put("tahun", source.tahun());
        putTextOrNull(context, "kode_tujuan", kodeTujuan);
        putTextOrNull(context, "tujuan", tujuan);
        context.put("kode_sasaran", kodeSasaran);
        context.put("sasaran", sasaran);
        putTextOrNull(context, "kode_indikator", optionalText(source.kodeIndikator(), 128));
        putTextOrNull(context, "indikator", optionalText(source.indikator(), 2000));
        putNumberOrNull(context, "target", source.target());
        putTextOrNull(context, "satuan", optionalText(source.satuan(), 100));
        putNumberOrNull(context, "pagu", source.pagu());
        putTextOrNull(context, "pemilik_risiko", optionalText(source.pemilikRisiko(), 500));

        return new ResolvedContext(context, sha256(context));
    }

    private static void putTextOrNull(ObjectNode target, String field, String value) {
        if (value == null) target.putNull(field);
        else target.put(field, value);
    }

    private static void putNumberOrNull(ObjectNode target, String field, BigDecimal value) {
        if (value == null) {
            target.putNull(field);
            return;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        target.put(field, normalized.scale() < 0 ? normalized.setScale(0) : normalized);
    }

    private String requiredText(String value, int maxLength) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > maxLength) throw contextInvalid();
        return value.trim();
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.trim().length() > maxLength) throw contextInvalid();
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private AiException contextInvalid() {
        return new AiException(400, "AI_CONTEXT_INVALID", "Konteks sasaran tidak lengkap atau tidak valid.");
    }

    private String sha256(ObjectNode context) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                  .digest(objectMapper.writeValueAsString(context).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new AiException(500, "AI_CONTEXT_HASH_FAILED", "Konteks AI tidak dapat divalidasi.", exception);
        }
    }

    public record ResolvedContext(ObjectNode value, String hash) {}
}
