package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.VerifikasiRisiko;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VerifikasiRisikoRepository extends JpaRepository<VerifikasiRisiko, Long> {
    Optional<VerifikasiRisiko> findByKodeRisiko(String kodeRisiko);

    boolean existsByRisikoOperasionalId(Long risikoOperasionalId);
    boolean existsByRisikoOpdId(Long risikoOpdId);
    boolean existsByRisikoPemdaId(Long risikoPemdaId);

    List<VerifikasiRisiko> findAllByStatusOrderByCreatedAtDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VerifikasiRisiko v where v.kodeRisiko = :kodeRisiko")
    Optional<VerifikasiRisiko> findLockedByKodeRisiko(@Param("kodeRisiko") String kodeRisiko);
}
