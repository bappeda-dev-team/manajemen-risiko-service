package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoPemda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoPemdaRepository extends JpaRepository<RisikoPemda, Long> {

    Optional<RisikoPemda> findByKodeRisiko(String kodeRisiko);

    List<RisikoPemda> findByKodeSasaranPemdaOrderByIdAsc(String kodeSasaranPemda);

}
