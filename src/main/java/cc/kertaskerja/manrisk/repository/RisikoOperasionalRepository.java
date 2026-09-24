package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoOperasionalRepository extends JpaRepository<RisikoOperasional, Long> {
    Optional<RisikoOperasional> findByKodeRisiko(String kodeRisiko);

    List<RisikoOperasional> findByKodeRekinOrderByIdAsc(String kodeRekin);
}
