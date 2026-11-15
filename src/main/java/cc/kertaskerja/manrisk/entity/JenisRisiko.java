package cc.kertaskerja.manrisk.entity;

import cc.kertaskerja.manrisk.common.BaseAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jenis_risiko")
@Getter
@Setter
@NoArgsConstructor
public class JenisRisiko extends BaseAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nama", nullable = false, length = 120)
    private String nama;
}
