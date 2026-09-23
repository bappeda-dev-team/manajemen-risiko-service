package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.Risiko;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoRepository extends JpaRepository<Risiko, Long> {

    Optional<Risiko> findByKodeRisiko(String kodeRisiko);

    List<Risiko> findByKodeSasaranOpd(String kodeSasaranOpd);
}
