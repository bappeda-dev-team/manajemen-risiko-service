package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.exception.AiException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

enum RisikoAiPromptTemplate {
    PERMASALAHAN("permasalahan",
          List.of("scope", "tahun", "tujuan", "sasaran", "indikator", "target", "satuan"),
          List.of("permasalahan", "sebab_permasalahan"), Set.of()),
    PERNYATAAN_RISIKO("pernyataan-risiko",
          List.of("scope", "sasaran", "indikator", "target", "satuan"),
          List.of("permasalahan", "sebab_permasalahan"), Set.of("permasalahan", "sebab_permasalahan")),
    RTP("rtp", List.of("scope"),
          List.of("pernyataan_risiko", "permasalahan", "sebab_permasalahan"), Set.of("pernyataan_risiko")),
    DAMPAK("dampak", List.of("scope", "sasaran", "indikator"),
          List.of("pernyataan_risiko", "permasalahan", "sebab_permasalahan", "skala_kemungkinan", "skala_dampak"),
          Set.of("pernyataan_risiko")),
    METODE_PEMANTAUAN("metode-pemantauan", List.of("scope", "sasaran"),
          List.of("pernyataan_risiko", "rencana_tindak_pengendalian"),
          Set.of("pernyataan_risiko", "rencana_tindak_pengendalian")),
    PENGENDALIAN_YANG_SUDAH_ADA("pengendalian-yang-sudah-ada", List.of("scope"),
          List.of("pernyataan_risiko"), Set.of("pernyataan_risiko")),
    REALISASI_TINDAK_PENGENDALIAN("realisasi-tindak-pengendalian", List.of("scope"),
          List.of("rencana_tindak_pengendalian"), Set.of("rencana_tindak_pengendalian"));

    static final String VERSION = "v1";

    private final String type;
    private final List<String> contextFields;
    private final List<String> inputFields;
    private final Set<String> requiredInput;

    RisikoAiPromptTemplate(String type, List<String> contextFields, List<String> inputFields,
                           Set<String> requiredInput) {
        this.type = type;
        this.contextFields = contextFields;
        this.inputFields = inputFields;
        this.requiredInput = requiredInput;
    }

    static RisikoAiPromptTemplate fromType(String type) {
        return Arrays.stream(values()).filter(template -> template.type.equals(type)).findFirst()
              .orElseThrow(() -> new AiException(400, "AI_TYPE_INVALID", "Tipe generate AI tidak dikenali."));
    }

    String type() { return type; }
    String templateId() { return "risiko/" + type; }
    String version() { return VERSION; }
    String resourcePath() { return "prompts/risiko/" + VERSION + "/" + type + ".md"; }
    List<String> contextFields() { return contextFields; }
    List<String> inputFields() { return inputFields; }
    Set<String> requiredInput() { return requiredInput; }
}
