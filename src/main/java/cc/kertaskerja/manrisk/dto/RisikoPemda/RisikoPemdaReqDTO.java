package cc.kertaskerja.manrisk.dto.RisikoPemda;

import cc.kertaskerja.manrisk.common.validation.CommaSeparatedList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class RisikoPemdaReqDTO {

    @JsonProperty("tahun")
    @NotNull @Min(1900) @Max(2100)
    private Integer tahun;

    @JsonProperty("kode_sasaran_pemda")
    @NotBlank @Size(max = 128)
    private String kodeSasaranPemda;

    @JsonProperty("permasalahan")
    @Size(max = 2000)
    private String permasalahan;

    @JsonProperty("sebab_permasalahan")
    @Size(max = 2000)
    private String sebabPermasalahan;

    @JsonProperty("pernyataan_risiko")
    @NotBlank @Size(max = 2000)
    private String pernyataanRisiko;

    @JsonProperty("skala_kemungkinan")
    @NotNull @Min(1) @Max(5)
    private Integer skalaKemungkinan;

    @JsonProperty("skala_dampak")
    @NotNull @Min(1) @Max(5)
    private Integer skalaDampak;

    @JsonProperty("pihak_terkena_risiko")
    @Size(max = 2000)
    private String pihakTerkenaRisiko;

    @JsonProperty("rencana_tindak_pengendalian")
    @NotBlank @Size(max = 2000)
    private String rencanaTindakPengendalian;

    @JsonProperty("metode_pemantauan")
    @Size(max = 2000)
    private String metodePemantauan;

    @JsonProperty("penanggungjawab_pemantauan")
    @Size(max = 500)
    private String penanggungjawabPemantauan;

    @JsonProperty("keterangan")
    @Size(max = 2000)
    private String keterangan;

    @JsonProperty("realisasi_tindak_pengendalian")
    @Size(max = 2000)
    private String realisasiTindakPengendalian;

    @JsonProperty("dapat_terkendali")
    @Size(max = 100)
    private String dapatTerkendali;

    @JsonProperty("dampak")
    @Size(max = 2000)
    private String dampak;

    @JsonProperty("catatan")
    @Size(max = 2000)
    private String catatan;

    @JsonProperty("perangkat_yang_menangani")
    @Size(max = 500)
    private String perangkatYangMenangani;

    @JsonProperty("kode_perangkat_yang_menangani")
    @NotBlank
    @Size(max = 1000)
    @CommaSeparatedList(maxItems = 5)
    private String kodePerangkatYangMenangani;

    @JsonProperty("pengendalian_yang_sudah_ada")
    @Size(max = 2000)
    private String pengendalianYangSudahAda;

    @JsonProperty("risiko_terjadi")
    private Boolean risikoTerjadi;

    @JsonProperty("waktu_terjadi")
    @PastOrPresent
    private LocalDate waktuTerjadi;
}
