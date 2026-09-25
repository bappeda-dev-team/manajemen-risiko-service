package cc.kertaskerja.manrisk.dto.RisikoPemda;

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
public class RisikoPemdaResDTO {
    @JsonProperty("scope") private String scope;
    @JsonProperty("id") private Long id;
    @JsonProperty("kode_risiko") private String kodeRisiko;
    @JsonProperty("tahun") private Integer tahun;
    @JsonProperty("kode_sasaran_pemda") private String kodeSasaranPemda;
    @JsonProperty("sasaran_pemda") private String sasaranPemda;
    @JsonProperty("periode") private String periode;
    @JsonProperty("indikators") private List<Indikator> indikators;
    @JsonProperty("risiko") private List<RisikoItem> risiko;
    @JsonProperty("permasalahan") private String permasalahan;
    @JsonProperty("sebab_permasalahan") private String sebabPermasalahan;
    @JsonProperty("pernyataan_risiko") private String pernyataanRisiko;
    @JsonProperty("skala_kemungkinan") private Integer skalaKemungkinan;
    @JsonProperty("skala_dampak") private Integer skalaDampak;
    @JsonProperty("pihak_terkena_risiko") private String pihakTerkenaRisiko;
    @JsonProperty("rencana_tindak_pengendalian") private String rencanaTindakPengendalian;
    @JsonProperty("metode_pemantauan") private String metodePemantauan;
    @JsonProperty("penanggungjawab_pemantauan") private String penanggungjawabPemantauan;
    @JsonProperty("keterangan") private String keterangan;
    @JsonProperty("realisasi_tindak_pengendalian") private String realisasiTindakPengendalian;
    @JsonProperty("dapat_terkendali") private String dapatTerkendali;
    @JsonProperty("dampak") private String dampak;
    @JsonProperty("catatan") private String catatan;
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
    public static class Indikator {
        @JsonProperty("id") private Long id;
        @JsonProperty("kode_indikator") private String kodeIndikator;
        @JsonProperty("indikator") private String indikator;
        @JsonProperty("rumus_perhitungan") private String rumusPerhitungan;
        @JsonProperty("sumber_data") private String sumberData;
        @JsonProperty("definisi_operasional") private String definisiOperasional;
        @JsonProperty("tahun_aktif") private Integer tahunAktif;
        @JsonProperty("targets") private List<Target> targets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Target {
        @JsonProperty("id") private Long id;
        @JsonProperty("kode_target") private String kodeTarget;
        @JsonProperty("tahun") private Integer tahun;
        @JsonProperty("target") private Number target;
        @JsonProperty("satuan") private String satuan;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RisikoItem {
        @JsonProperty("id") private Long id;
        @JsonProperty("kode_risiko") private String kodeRisiko;
        @JsonProperty("type") private String type;
        @JsonProperty("permasalahan") private String permasalahan;
        @JsonProperty("sebab_permasalahan") private String sebabPermasalahan;
        @JsonProperty("pernyataan_risiko") private String pernyataanRisiko;
        @JsonProperty("skala_kemungkinan") private Integer skalaKemungkinan;
        @JsonProperty("skala_dampak") private Integer skalaDampak;
        @JsonProperty("pihak_terkena_risiko") private String pihakTerkenaRisiko;
        @JsonProperty("rencana_tindak_pengendalian") private String rencanaTindakPengendalian;
        @JsonProperty("metode_pemantauan") private String metodePemantauan;
        @JsonProperty("penanggungjawab_pemantauan") private String penanggungjawabPemantauan;
        @JsonProperty("keterangan") private String keterangan;
        @JsonProperty("realisasi_tindak_pengendalian") private String realisasiTindakPengendalian;
        @JsonProperty("dapat_terkendali") private String dapatTerkendali;
        @JsonProperty("dampak") private String dampak;
        @JsonProperty("catatan") private String catatan;
        @JsonProperty("perangkat_yang_menangani") private String perangkatYangMenangani;
        @JsonProperty("kode_perangkat_yang_menangani") private String kodePerangkatYangMenangani;
        @JsonProperty("pengendalian_yang_sudah_ada") private String pengendalianYangSudahAda;
        @JsonProperty("risiko_terjadi") private Boolean risikoTerjadi;
        @JsonProperty("waktu_terjadi") private LocalDate waktuTerjadi;
        @JsonProperty("created_at") private LocalDateTime createdAt;
        @JsonProperty("updated_at") private LocalDateTime updatedAt;
    }
}
