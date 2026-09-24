package cc.kertaskerja.manrisk.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = false)
public record GenerateAiReqDTO(
        @NotBlank @Size(max = 64) String requestId,
        @NotBlank @Size(max = 64) String type,
        @NotNull @Valid Context context,
        @NotNull Map<String, String> input
) {
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Context(
            @Pattern(regexp = "(?i)opd|pemda|operasional") String scope,
            @JsonProperty("kode_opd") @Size(max = 128) String kodeOpd,
            @NotNull @Min(1900) @Max(2100) Integer tahun,
            @JsonProperty("kode_tujuan_opd") @Size(max = 128) String kodeTujuanOpd,
            @JsonProperty("tujuan_opd") @Size(max = 2000) String tujuanOpd,
            @JsonProperty("kode_sasaran_opd") @Size(max = 128) String kodeSasaranOpd,
            @JsonProperty("sasaran_opd") @Size(max = 2000) String sasaranOpd,
            @JsonProperty("kode_tujuan_pemda") @Size(max = 128) String kodeTujuanPemda,
            @JsonProperty("tujuan_pemda") @Size(max = 2000) String tujuanPemda,
            @JsonProperty("kode_sasaran_pemda") @Size(max = 128) String kodeSasaranPemda,
            @JsonProperty("sasaran_pemda") @Size(max = 2000) String sasaranPemda,
            @JsonProperty("kode_indikator") @Size(max = 128) String kodeIndikator,
            @Size(max = 2000) String indikator,
            BigDecimal target,
            @Size(max = 100) String satuan,
            @PositiveOrZero BigDecimal pagu,
            @JsonProperty("pemilik_risiko") @Size(max = 500) String pemilikRisiko,
            @JsonProperty("kode_rekin") @Size(max = 128) String kodeRekin,
            @Size(max = 2000) String rekin,
            @JsonProperty("pegawai_id") @Size(max = 128) String pegawaiId
    ) {
        public Context(String scope, String kodeOpd, Integer tahun,
                       String kodeTujuanOpd, String tujuanOpd, String kodeSasaranOpd, String sasaranOpd,
                       String kodeTujuanPemda, String tujuanPemda, String kodeSasaranPemda, String sasaranPemda,
                       String kodeIndikator, String indikator, BigDecimal target, String satuan,
                       BigDecimal pagu, String pemilikRisiko) {
            this(scope, kodeOpd, tahun, kodeTujuanOpd, tujuanOpd, kodeSasaranOpd, sasaranOpd,
                  kodeTujuanPemda, tujuanPemda, kodeSasaranPemda, sasaranPemda, kodeIndikator,
                  indikator, target, satuan, pagu, pemilikRisiko, null, null, null);
        }
    }
}
