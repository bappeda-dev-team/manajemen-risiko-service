package cc.kertaskerja.manrisk.dto.Risiko;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RisikoResDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("kode_opd")
    private String kodeOpd;

    @JsonProperty("kode_risiko")
    private String kodeRisiko;

    @JsonProperty("tahun")
    private Integer tahun;

    @JsonProperty("kode_sasaran_opd")
    private String kodeSasaranOpd;

    @JsonProperty("pernyataan_risiko")
    private String pernyataanRisiko;

    @JsonProperty("skala_kemungkinan")
    private Integer skalaKemungkinan;

    @JsonProperty("skala_dampak")
    private Integer skalaDampak;

    @JsonProperty("pihak_terkena_risiko")
    private String pihakTerkenaRisiko;

    @JsonProperty("rencana_tindak_pengendalian")
    private String rencanaTindakPengendalian;

    @JsonProperty("metode_pemantauan")
    private String metodePemantauan;

    @JsonProperty("penanggungjawab_pemantauan")
    private String penanggungjawabPemantauan;

    @JsonProperty("keterangan")
    private String keterangan;

    @JsonProperty("realisasi_tindak_pengendalian")
    private String realisasiTindakPengendalian;

    @JsonProperty("dapat_terkendali")
    private String dapatTerkendali;

    @JsonProperty("dampak")
    private String dampak;

    @JsonProperty("catatan")
    private String catatan;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
