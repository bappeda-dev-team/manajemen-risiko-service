package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.JenisRisiko;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JenisRisikoRepository extends JpaRepository<JenisRisiko, Long> {
    List<JenisRisiko> findAllByOrderByIdAsc();

    boolean existsByNamaIgnoreCase(String nama);

    boolean existsByNamaIgnoreCaseAndIdNot(String nama, Long id);
}
