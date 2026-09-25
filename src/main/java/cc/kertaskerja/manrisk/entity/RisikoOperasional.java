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
@Table(name = "risiko_operasional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RisikoOperasional extends BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_risiko", unique = true)
    private String kodeRisiko;

    @Column(name = "tahun", nullable = false)
    private Integer tahun;

    @Column(name = "kode_rekin", nullable = false, length = 128)
    private String kodeRekin;

    @Column(name = "kode_opd", nullable = false, length = 128)
    private String kodeOpd;

    @Column(name = "pegawai_id", nullable = false, length = 128)
    private String pegawaiId;

    @Column(name = "permasalahan", columnDefinition = "TEXT")
    private String permasalahan;

    @Column(name = "sebab_permasalahan", columnDefinition = "TEXT")
    private String sebabPermasalahan;

    @Column(name = "pernyataan_risiko", nullable = false, columnDefinition = "TEXT")
    private String pernyataanRisiko;

    @Column(name = "skala_kemungkinan", nullable = false)
    private Integer skalaKemungkinan;

    @Column(name = "skala_dampak", nullable = false)
    private Integer skalaDampak;

    @Column(name = "pihak_terkena_risiko", columnDefinition = "TEXT")
    private String pihakTerkenaRisiko;

    @Column(name = "rencana_tindak_pengendalian", nullable = false, columnDefinition = "TEXT")
    private String rencanaTindakPengendalian;

    @Column(name = "metode_pemantauan", columnDefinition = "TEXT")
    private String metodePemantauan;

    @Column(name = "penanggungjawab_pemantauan", columnDefinition = "TEXT")
    private String penanggungjawabPemantauan;

    @Column(name = "keterangan", columnDefinition = "TEXT")
    private String keterangan;

    @Column(name = "realisasi_tindak_pengendalian", columnDefinition = "TEXT")
    private String realisasiTindakPengendalian;

    @Column(name = "dapat_terkendali")
    private String dapatTerkendali;

    @Column(name = "dampak", columnDefinition = "TEXT")
    private String dampak;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "perangkat_yang_menangani", columnDefinition = "TEXT")
    private String perangkatYangMenangani;

    @Column(name = "kode_perangkat_yang_menangani", nullable = false, columnDefinition = "TEXT")
    private String kodePerangkatYangMenangani;

    @Column(name = "pengendalian_yang_sudah_ada", columnDefinition = "TEXT")
    private String pengendalianYangSudahAda;

    @Column(name = "risiko_terjadi")
    private Boolean risikoTerjadi;

    @Column(name = "waktu_terjadi")
    private LocalDate waktuTerjadi;
}
