package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoOpd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RisikoOpdRepository extends JpaRepository<RisikoOpd, Long> {

    Optional<RisikoOpd> findByKodeRisiko(String kodeRisiko);

    List<RisikoOpd> findByKodeSasaranOpd(String kodeSasaranOpd);
}
