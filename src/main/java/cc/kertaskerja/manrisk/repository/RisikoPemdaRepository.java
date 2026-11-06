package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoPemda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoPemdaRepository extends JpaRepository<RisikoPemda, Long> {

    Optional<RisikoPemda> findByKodeRisiko(String kodeRisiko);

    List<RisikoPemda> findByKodeSasaranPemdaOrderByIdAsc(String kodeSasaranPemda);

    List<RisikoPemda> findByTahunOrderByKodeSasaranPemdaAscIdAsc(Integer tahun);

    List<RisikoPemda> findByTahunAndKodeSasaranPemdaInOrderByKodeSasaranPemdaAscIdAsc(
          Integer tahun, List<String> kodeSasaran);

    List<RisikoPemda> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDesc(String createdBy);

    List<RisikoPemda> findByKodeOpdAndDeletedAtIsNullOrderByCreatedAtDesc(String kodeOpd);

    @Query("""
          select r from RisikoPemda r where r.tahun = :tahun and r.deletedAt is null
          and exists (select v.id from VerifikasiRisiko v where v.risikoPemda = r and v.status = 'VERIFIED')
          order by r.kodeSasaranPemda asc, r.id asc
          """)
    List<RisikoPemda> findVerifiedForLaporan(@Param("tahun") Integer tahun);

    @Query("""
          select r from RisikoPemda r where r.tahun = :tahun and r.kodeSasaranPemda in :kodeSasaran and r.deletedAt is null
          and exists (select v.id from VerifikasiRisiko v where v.risikoPemda = r and v.status = 'VERIFIED')
          order by r.kodeSasaranPemda asc, r.id asc
          """)
    List<RisikoPemda> findVerifiedForLaporanBySasaran(@Param("tahun") Integer tahun,
                                                      @Param("kodeSasaran") List<String> kodeSasaran);

}
