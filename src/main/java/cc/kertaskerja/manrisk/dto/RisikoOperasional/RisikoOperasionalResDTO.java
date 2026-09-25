package cc.kertaskerja.manrisk.dto.RisikoOperasional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RisikoOperasionalResDTO {
    private String scope;
    private Long id;

    @JsonProperty("kode_risiko") private String kodeRisiko;
    private Integer tahun;
    @JsonProperty("kode_rekin") private String kodeRekin;
    @JsonProperty("kode_opd") private String kodeOpd;
    @JsonProperty("pegawai_id") private String pegawaiId;
    private List<RisikoItem> risiko;
    private String permasalahan;
    @JsonProperty("sebab_permasalahan") private String sebabPermasalahan;
    @JsonProperty("pernyataan_risiko") private String pernyataanRisiko;
    @JsonProperty("skala_kemungkinan") private Integer skalaKemungkinan;
    @JsonProperty("skala_dampak") private Integer skalaDampak;
    @JsonProperty("pihak_terkena_risiko") private String pihakTerkenaRisiko;
    @JsonProperty("rencana_tindak_pengendalian") private String rencanaTindakPengendalian;
    @JsonProperty("metode_pemantauan") private String metodePemantauan;
    @JsonProperty("penanggungjawab_pemantauan") private String penanggungjawabPemantauan;
    private String keterangan;
    @JsonProperty("realisasi_tindak_pengendalian") private String realisasiTindakPengendalian;
    @JsonProperty("dapat_terkendali") private String dapatTerkendali;
    private String dampak;
    private String catatan;
    @JsonProperty("perangkat_yang_menangani") private String perangkatYangMenangani;
    @JsonProperty("kode_perangkat_yang_menangani") private String kodePerangkatYangMenangani;
    @JsonProperty("pengendalian_yang_sudah_ada") private String pengendalianYangSudahAda;
    @JsonProperty("risiko_terjadi") private Boolean risikoTerjadi;
    @JsonProperty("waktu_terjadi") private LocalDate waktuTerjadi;
    @JsonProperty("created_at") private LocalDateTime createdAt;
    @JsonProperty("updated_at") private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RisikoItem {
        private Long id;
        @JsonProperty("kode_risiko") private String kodeRisiko;
        private String type;
        private String permasalahan;
        @JsonProperty("sebab_permasalahan") private String sebabPermasalahan;
        @JsonProperty("pernyataan_risiko") private String pernyataanRisiko;
        @JsonProperty("skala_kemungkinan") private Integer skalaKemungkinan;
        @JsonProperty("skala_dampak") private Integer skalaDampak;
        @JsonProperty("pihak_terkena_risiko") private String pihakTerkenaRisiko;
        @JsonProperty("rencana_tindak_pengendalian") private String rencanaTindakPengendalian;
        @JsonProperty("metode_pemantauan") private String metodePemantauan;
        @JsonProperty("penanggungjawab_pemantauan") private String penanggungjawabPemantauan;
        private String keterangan;
        @JsonProperty("realisasi_tindak_pengendalian") private String realisasiTindakPengendalian;
        @JsonProperty("dapat_terkendali") private String dapatTerkendali;
        private String dampak;
        private String catatan;
        @JsonProperty("perangkat_yang_menangani") private String perangkatYangMenangani;
        @JsonProperty("kode_perangkat_yang_menangani") private String kodePerangkatYangMenangani;
        @JsonProperty("pengendalian_yang_sudah_ada") private String pengendalianYangSudahAda;
        @JsonProperty("risiko_terjadi") private Boolean risikoTerjadi;
        @JsonProperty("waktu_terjadi") private LocalDate waktuTerjadi;
        @JsonProperty("created_at") private LocalDateTime createdAt;
        @JsonProperty("updated_at") private LocalDateTime updatedAt;
    }
}
