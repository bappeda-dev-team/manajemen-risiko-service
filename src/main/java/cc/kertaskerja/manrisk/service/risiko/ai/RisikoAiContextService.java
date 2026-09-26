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
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RisikoAiContextService {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    private final ObjectMapper objectMapper;

    /**
     * Context frontend adalah snapshot tidak tepercaya
     * yang hanya digunakan untuk prompt AI.
     */
    public ResolvedContext normalize(GenerateAiReqDTO.Context source) {
        if (source == null) {
            throw contextInvalid();
        }

        String scope = normalizeScope(source.scope());

        validateGeneralContext(source);

        ScopeContext scopeContext = resolveScopeContext(source, scope);
        ObjectNode context = createContext(source, scope, scopeContext);

        return new ResolvedContext(context, sha256(context));
    }

    private String normalizeScope(String value) {
        String scope = value == null || value.isBlank()
                ? "opd"
                : value.trim().toLowerCase(Locale.ROOT);

        return switch (scope) {
            case "opd", "pemda", "operasional" -> scope;
            default -> throw contextInvalid();
        };
    }

    private void validateGeneralContext(GenerateAiReqDTO.Context source) {
        if (source.tahun() == null || source.tahun() < MIN_YEAR || source.tahun() > MAX_YEAR) {
            throw contextInvalid();
        }

        if (source.pagu() != null && source.pagu().signum() < 0) {
            throw contextInvalid();
        }
    }

    private ScopeContext resolveScopeContext(GenerateAiReqDTO.Context source, String scope) {
        return switch (scope) {
            case "pemda" -> resolvePemdaContext(source);
            case "operasional" -> resolveOperasionalContext(source);
            default -> resolveOpdContext(source);
        };
    }

    private ScopeContext resolvePemdaContext(GenerateAiReqDTO.Context source) {
        if (hasText(source.kodeOpd()) || hasOpdSasaranContext(source) || hasOperasionalContext(source)) {
            throw contextInvalid();
        }

        String kodeTujuan = optionalText(source.kodeTujuanPemda(), 128);
        String tujuan = optionalText(source.tujuanPemda(), 2000);
        String kodeSasaran = requiredText(source.kodeSasaranPemda(), 128);
        String sasaran = requiredText(source.sasaranPemda(), 2000);

        return new ScopeContext(null, null, null, null, kodeTujuan, tujuan, kodeSasaran, sasaran);
    }

    private ScopeContext resolveOperasionalContext(GenerateAiReqDTO.Context source) {
        if (hasOpdSasaranContext(source) || hasPemdaContext(source)) {
            throw contextInvalid();
        }

        String kodeOpd = requiredText(source.kodeOpd(), 128);
        String pegawaiId = requiredText(source.pegawaiId(), 128);
        String kodeRekin = requiredText(source.kodeRekin(), 128);
        String rekin = requiredText(source.rekin(), 2000);

        return new ScopeContext(kodeOpd, pegawaiId, kodeRekin, rekin, null, null, kodeRekin, rekin);
    }

    private ScopeContext resolveOpdContext(GenerateAiReqDTO.Context source) {
        if (hasPemdaContext(source) || hasOperasionalContext(source)) {
            throw contextInvalid();
        }

        String kodeOpd = requiredText(source.kodeOpd(), 128);
        String kodeTujuan = optionalText(source.kodeTujuanOpd(), 128);
        String tujuan = optionalText(source.tujuanOpd(), 2000);
        String kodeSasaran = requiredText(source.kodeSasaranOpd(), 128);
        String sasaran = requiredText(source.sasaranOpd(), 2000);

        return new ScopeContext(kodeOpd, null, null, null, kodeTujuan, tujuan, kodeSasaran, sasaran);
    }

    private boolean hasOpdSasaranContext(GenerateAiReqDTO.Context source) {
        return hasText(source.kodeTujuanOpd())
                || hasText(source.tujuanOpd())
                || hasText(source.kodeSasaranOpd())
                || hasText(source.sasaranOpd());
    }

    private boolean hasPemdaContext(GenerateAiReqDTO.Context source) {
        return hasText(source.kodeTujuanPemda())
                || hasText(source.tujuanPemda())
                || hasText(source.kodeSasaranPemda())
                || hasText(source.sasaranPemda());
    }

    private boolean hasOperasionalContext(GenerateAiReqDTO.Context source) {
        return hasText(source.kodeRekin())
                || hasText(source.rekin())
                || hasText(source.pegawaiId());
    }

    private ObjectNode createContext(GenerateAiReqDTO.Context source, String scope, ScopeContext values) {
        ObjectNode context = objectMapper.createObjectNode();

        context.put("scope", scope);
        putTextOrNull(context, "kode_opd", values.kodeOpd());
        putTextOrNull(context, "pegawai_id", values.pegawaiId());
        putTextOrNull(context, "kode_rekin", values.kodeRekin());
        putTextOrNull(context, "rekin", values.rekin());

        context.put("tahun", source.tahun());
        putTextOrNull(context, "kode_tujuan", values.kodeTujuan());
        putTextOrNull(context, "tujuan", values.tujuan());
        context.put("kode_sasaran", values.kodeSasaran());
        context.put("sasaran", values.sasaran());

        putTextOrNull(context, "kode_indikator", optionalText(source.kodeIndikator(), 128));
        putTextOrNull(context, "indikator", optionalText(source.indikator(), 2000));
        putNumberOrNull(context, "target", source.target());
        putTextOrNull(context, "satuan", optionalText(source.satuan(), 100));
        putNumberOrNull(context, "pagu", source.pagu());
        putTextOrNull(context, "pemilik_risiko", optionalText(source.pemilikRisiko(), 500));

        return context;
    }

    private static void putTextOrNull(ObjectNode target, String field, String value) {
        if (value == null) {
            target.putNull(field);
        } else {
            target.put(field, value);
        }
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
        String normalized = normalizeText(value);

        if (normalized == null || normalized.length() > maxLength) {
            throw contextInvalid();
        }

        return normalized;
    }

    private String optionalText(String value, int maxLength) {
        String normalized = normalizeText(value);

        if (normalized == null) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw contextInvalid();
        }

        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String sha256(ObjectNode context) {
        try {
            String serializedContext = objectMapper.writeValueAsString(context);
            byte[] content = serializedContext.getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new AiException(500, "AI_CONTEXT_HASH_FAILED", "Konteks AI tidak dapat divalidasi.", exception);
        }
    }

    private AiException contextInvalid() {
        return new AiException(400, "AI_CONTEXT_INVALID", "Konteks sasaran tidak lengkap atau tidak valid.");
    }

    private record ScopeContext(
            String kodeOpd,
            String pegawaiId,
            String kodeRekin,
            String rekin,
            String kodeTujuan,
            String tujuan,
            String kodeSasaran,
            String sasaran
    ) {
    }

    public record ResolvedContext(ObjectNode value, String hash) {
    }
}