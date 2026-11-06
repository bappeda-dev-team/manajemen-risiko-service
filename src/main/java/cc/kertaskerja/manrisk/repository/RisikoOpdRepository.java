package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoOpd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoOpdRepository extends JpaRepository<RisikoOpd, Long> {

    Optional<RisikoOpd> findByKodeRisiko(String kodeRisiko);

    List<RisikoOpd> findByKodeSasaranOpdOrderByIdAsc(String kodeSasaranOpd);

    List<RisikoOpd> findByKodeOpdAndTahunOrderByKodeSasaranOpdAscIdAsc(String kodeOpd, Integer tahun);

    List<RisikoOpd> findByKodeOpdAndTahunAndKodeSasaranOpdInOrderByKodeSasaranOpdAscIdAsc(
          String kodeOpd, Integer tahun, List<String> kodeSasaran);

    List<RisikoOpd> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDesc(String createdBy);

    List<RisikoOpd> findByKodeOpdAndDeletedAtIsNullOrderByCreatedAtDesc(String kodeOpd);

    @Query("""
          select r from RisikoOpd r where r.kodeOpd = :kodeOpd and r.tahun = :tahun and r.deletedAt is null
          and exists (select v.id from VerifikasiRisiko v where v.risikoOpd = r and v.status = 'VERIFIED')
          order by r.kodeSasaranOpd asc, r.id asc
          """)
    List<RisikoOpd> findVerifiedForLaporan(@Param("kodeOpd") String kodeOpd, @Param("tahun") Integer tahun);

    @Query("""
          select r from RisikoOpd r where r.kodeOpd = :kodeOpd and r.tahun = :tahun and r.kodeSasaranOpd in :kodeSasaran
          and r.deletedAt is null and exists (select v.id from VerifikasiRisiko v where v.risikoOpd = r and v.status = 'VERIFIED')
          order by r.kodeSasaranOpd asc, r.id asc
          """)
    List<RisikoOpd> findVerifiedForLaporanBySasaran(@Param("kodeOpd") String kodeOpd, @Param("tahun") Integer tahun,
                                                    @Param("kodeSasaran") List<String> kodeSasaran);
}
