package cc.kertaskerja.manrisk.entity;

import cc.kertaskerja.manrisk.common.BaseAuditable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risiko")
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risiko extends BaseAuditable {

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
}
