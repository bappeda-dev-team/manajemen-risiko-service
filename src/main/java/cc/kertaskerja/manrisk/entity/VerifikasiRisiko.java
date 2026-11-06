package cc.kertaskerja.manrisk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "verifikasi_risiko")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifikasiRisiko {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kode_risiko", nullable = false, unique = true)
    private String kodeRisiko;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risiko_operasional_id")
    private RisikoOperasional risikoOperasional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risiko_opd_id")
    private RisikoOpd risikoOpd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risiko_pemda_id")
    private RisikoPemda risikoPemda;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "verifikator_id")
    private String verifikatorId;

    @Column(name = "verifikator_nama")
    private String verifikatorNama;

    @Column(columnDefinition = "TEXT")
    private String catatan;
}
