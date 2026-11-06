package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoOperasionalRepository extends JpaRepository<RisikoOperasional, Long> {
    Optional<RisikoOperasional> findByKodeRisiko(String kodeRisiko);

    List<RisikoOperasional> findByKodeRekinOrderByIdAsc(String kodeRekin);

    List<RisikoOperasional> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDesc(String createdBy);

    List<RisikoOperasional> findByKodeOpdAndDeletedAtIsNullOrderByCreatedAtDesc(String kodeOpd);

    @Query("""
          select r from RisikoOperasional r where r.kodeOpd = :kodeOpd and r.tahun = :tahun and r.deletedAt is null
          and exists (select v.id from VerifikasiRisiko v where v.risikoOperasional = r and v.status = 'VERIFIED')
          order by r.kodeRekin asc, r.id asc
          """)
    List<RisikoOperasional> findVerifiedForLaporan(@Param("kodeOpd") String kodeOpd, @Param("tahun") Integer tahun);
}
