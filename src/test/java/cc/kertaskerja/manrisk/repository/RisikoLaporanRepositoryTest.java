package cc.kertaskerja.manrisk.repository;

import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class RisikoLaporanRepositoryTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RisikoOpdRepository opdRepository;

    @Autowired
    private RisikoPemdaRepository pemdaRepository;

    @Test
    void opdQueriesFilterYearAndOwnerEvenWhenSasaranCodeIsReused() {
        insertOpd("OPD-A", 2026, "SAS-01", "risiko-A-2026-01");
        insertOpd("OPD-A", 2026, "SAS-01", "risiko-A-2026-02");
        insertOpd("OPD-A", 2026, "SAS-03", "risiko-A-2026-03");
        insertOpd("OPD-A", 2025, "SAS-01", "risiko-A-2025");
        insertOpd("OPD-B", 2026, "SAS-01", "risiko-B-2026");

        List<RisikoOpd> all = opdRepository.findByKodeOpdAndTahunOrderByKodeSasaranOpdAscIdAsc("OPD-A", 2026);
        List<RisikoOpd> selected = opdRepository
              .findByKodeOpdAndTahunAndKodeSasaranOpdInOrderByKodeSasaranOpdAscIdAsc(
                    "OPD-A", 2026, List.of("SAS-01", "SAS-EMPTY"));

        assertEquals(List.of("risiko-A-2026-01", "risiko-A-2026-02", "risiko-A-2026-03"),
              all.stream().map(RisikoOpd::getPernyataanRisiko).toList());
        assertEquals(List.of("risiko-A-2026-01", "risiko-A-2026-02"),
              selected.stream().map(RisikoOpd::getPernyataanRisiko).toList());
    }

    @Test
    void pemdaQueriesFilterYearAndSelectedSasaran() {
        insertPemda(2026, "SAS-PEM-01", "risiko-2026-01");
        insertPemda(2026, "SAS-PEM-03", "risiko-2026-03");
        insertPemda(2025, "SAS-PEM-01", "risiko-2025-01");

        List<RisikoPemda> all = pemdaRepository.findByTahunOrderByKodeSasaranPemdaAscIdAsc(2026);
        List<RisikoPemda> selected = pemdaRepository
              .findByTahunAndKodeSasaranPemdaInOrderByKodeSasaranPemdaAscIdAsc(
                    2026, List.of("SAS-PEM-03", "SAS-PEM-EMPTY"));

        assertEquals(List.of("risiko-2026-01", "risiko-2026-03"),
              all.stream().map(RisikoPemda::getPernyataanRisiko).toList());
        assertEquals(List.of("risiko-2026-03"),
              selected.stream().map(RisikoPemda::getPernyataanRisiko).toList());
    }

    @Test
    void migrationCreatesIndexesForBothContextQueries() {
        assertNotNull(jdbc.queryForObject(
              "SELECT to_regclass('public.idx_risiko_opd_laporan_context_sasaran_id')", String.class));
        assertNotNull(jdbc.queryForObject(
              "SELECT to_regclass('public.idx_risiko_pemda_laporan_tahun_sasaran_id')", String.class));
    }

    @Test
    void explainAnalyzeBothReportQueryVariantsOnRepresentativeData() {
        jdbc.update("""
              INSERT INTO risiko_opd (kode_opd, tahun, kode_sasaran_opd, pernyataan_risiko)
              SELECT CASE WHEN n % 7 = 0 THEN 'OPD-A' ELSE 'OPD-B' END,
                     2010 + n % 20,
                     'SAS-' || lpad((n % 50)::text, 2, '0'),
                     'Risiko ' || n
              FROM generate_series(1, 20000) AS n
              """);
        jdbc.update("""
              INSERT INTO risiko_pemda (
                  tahun, kode_sasaran_pemda, pernyataan_risiko, skala_kemungkinan,
                  skala_dampak, rencana_tindak_pengendalian, kode_perangkat_yang_menangani)
              SELECT 2010 + n % 20,
                     'SAS-PEM-' || lpad((n % 50)::text, 2, '0'),
                     'Risiko ' || n,
                     3, 4, 'RTP', 'OPD-A'
              FROM generate_series(1, 20000) AS n
              """);
        jdbc.execute("ANALYZE risiko_opd");
        jdbc.execute("ANALYZE risiko_pemda");

        List<String> queries = List.of(
              "SELECT id FROM risiko_opd WHERE kode_opd = 'OPD-A' AND tahun = 2026 ORDER BY kode_sasaran_opd, id",
              "SELECT id FROM risiko_opd WHERE kode_opd = 'OPD-A' AND tahun = 2026 AND kode_sasaran_opd IN ('SAS-06', 'SAS-26') ORDER BY kode_sasaran_opd, id",
              "SELECT id FROM risiko_pemda WHERE tahun = 2026 ORDER BY kode_sasaran_pemda, id",
              "SELECT id FROM risiko_pemda WHERE tahun = 2026 AND kode_sasaran_pemda IN ('SAS-PEM-06', 'SAS-PEM-26') ORDER BY kode_sasaran_pemda, id");
        for (int index = 0; index < queries.size(); index++) {
            String plan = String.join("\n", jdbc.queryForList(
                  "EXPLAIN (ANALYZE, BUFFERS) " + queries.get(index), String.class));
            assertFalse(plan.isBlank());
            String expectedIndex = index < 2
                  ? "idx_risiko_opd_laporan_context_sasaran_id"
                  : "idx_risiko_pemda_laporan_tahun_sasaran_id";
            assertTrue(plan.contains(expectedIndex), plan);
        }
    }

    private void insertOpd(String kodeOpd, int tahun, String kodeSasaran, String pernyataan) {
        jdbc.update("""
              INSERT INTO risiko_opd (kode_opd, tahun, kode_sasaran_opd, pernyataan_risiko)
              VALUES (?, ?, ?, ?)
              """, kodeOpd, tahun, kodeSasaran, pernyataan);
    }

    private void insertPemda(int tahun, String kodeSasaran, String pernyataan) {
        jdbc.update("""
              INSERT INTO risiko_pemda (
                  tahun, kode_sasaran_pemda, pernyataan_risiko, skala_kemungkinan,
                  skala_dampak, rencana_tindak_pengendalian, kode_perangkat_yang_menangani)
              VALUES (?, ?, ?, 3, 4, 'RTP', 'OPD-A')
              """, tahun, kodeSasaran, pernyataan);
    }
}
