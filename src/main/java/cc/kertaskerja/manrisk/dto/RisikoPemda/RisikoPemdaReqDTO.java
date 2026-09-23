package cc.kertaskerja.manrisk.dto.RisikoPemda;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RisikoPemdaReqDTO {

    @JsonProperty("tahun")
    private Integer tahun;

    @JsonProperty("kode_sasaran_pemda")
    private String kodeSasaranPemda;

    @JsonProperty("permasalahan")
    private String permasalahan;

    @JsonProperty("sebab_permasalahan")
    private String sebabPermasalahan;

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

    @JsonProperty("perangkat_yang_menangani")
    private String perangkatYangMenangani;

    @JsonProperty("kode_perangkat_yang_menangani")
    private String kodePerangkatYangMenangani;
}
