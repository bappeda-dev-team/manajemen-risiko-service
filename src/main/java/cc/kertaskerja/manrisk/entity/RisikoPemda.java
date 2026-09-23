package cc.kertaskerja.manrisk.entity;

import cc.kertaskerja.manrisk.common.BaseAuditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "risiko_pemda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RisikoPemda extends BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_risiko", unique = true)
    private String kodeRisiko;

    @Column(name = "tahun", nullable = false)
    private Integer tahun;

    @Column(name = "kode_sasaran_pemda", nullable = false, length = 128)
    private String kodeSasaranPemda;

    @Column(name = "permasalahan")
    private String permasalahan;

    @Column(name = "sebab_permasalahan")
    private String sebabPermasalahan;

    @Column(name = "pernyataan_risiko", nullable = false, columnDefinition = "TEXT")
    private String pernyataanRisiko;

    @Column(name = "skala_kemungkinan", nullable = false)
    private Integer skalaKemungkinan;

    @Column(name = "skala_dampak", nullable = false)
    private Integer skalaDampak;

    @Column(name = "pihak_terkena_risiko")
    private String pihakTerkenaRisiko;

    @Column(name = "rencana_tindak_pengendalian", nullable = false, columnDefinition = "TEXT")
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

    @Column(name = "kode_perangkat_yang_menangani", nullable = false, length = 128)
    private String kodePerangkatYangMenangani;
}
