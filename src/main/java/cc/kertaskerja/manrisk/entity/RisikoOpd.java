package cc.kertaskerja.manrisk.entity;

import cc.kertaskerja.manrisk.common.BaseAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "risiko_opd")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RisikoOpd extends BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_opd")
    private String kodeOpd;

    @Column(name = "kode_risiko")
    private String kodeRisiko;

    @Column(name = "tahun")
    private Integer tahun;

    @Column(name = "kode_sasaran_opd")
    private String kodeSasaranOpd;

    @Column(name = "permasalahan")
    private String permasalahan;

    @Column(name = "sebab_permasalahan")
    private String sebabPermasalahan;

    @Column(name = "pernyataan_risiko")
    private String pernyataanRisiko;

    @Column(name = "skala_kemungkinan")
    private Integer skalaKemungkinan;

    @Column(name = "skala_dampak")
    private Integer skalaDampak;

    @Column(name = "pihak_terkena_risiko")
    private String pihakTerkenaRisiko;

    @Column(name = "rencana_tindak_pengendalian")
    private String rencanaTindakPengendalian;

    @Column(name = "metode_pemantauan")
    private String metodePemantauan;

    @Column(name = "penanggungjawab_pemantauan")
    private String penanggungjawabPemantauan;

    @Column(name = "keterangan")
    private String keterangan;

    @Column(name = "realisasi_tindak_pengendalian")
    private String realisasiTindakPengendalian;

    @Column(name = "dapat_terkendali")
    private String dapatTerkendali;

    @Column(name = "dampak")
    private String dampak;

    @Column(name = "catatan")
    private String catatan;

    @Column(name = "perangkat_yang_menangani")
    private String perangkatYangMenangani;

    @Column(name = "kode_perangkat_yang_menangani", columnDefinition = "TEXT")
    private String kodePerangkatYangMenangani;

    @Column(name = "pengendalian_yang_sudah_ada", columnDefinition = "TEXT")
    private String pengendalianYangSudahAda;

    @Column(name = "risiko_terjadi")
    private Boolean risikoTerjadi;

    @Column(name = "waktu_terjadi")
    private LocalDate waktuTerjadi;
}
