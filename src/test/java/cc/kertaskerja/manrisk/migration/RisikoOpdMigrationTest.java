package cc.kertaskerja.manrisk.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class RisikoOpdMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
          new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void upgradesV11DataInPlaceAndKeepsIdentitySequenceHealthy() throws Exception {
        flywayAt(MigrationVersion.fromVersion("11")).migrate();

        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                  INSERT INTO risiko (
                      id, kode_opd, kode_risiko, tahun, kode_sasaran_opd,
                      pernyataan_risiko, skala_kemungkinan, skala_dampak,
                      rencana_tindak_pengendalian, created_at, updated_at
                  ) VALUES
                      (27, 'OPD-001', 'RSK-0027', 2026, 'SAS-001',
                       'Risiko lama', 3, 4, 'RTP lama',
                       TIMESTAMP '2026-01-02 03:04:05', TIMESTAMP '2026-02-03 04:05:06'),
                      (10000, 'OPD-001', 'RSK-10000', 2026, 'SAS-002',
                       'Risiko lima digit', 2, 5, 'RTP lima digit',
                       TIMESTAMP '2026-01-02 03:04:05', TIMESTAMP '2026-02-03 04:05:06')
                  """);
        }

        flywayAt(null).migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery(
                  "SELECT to_regclass('public.risiko'), to_regclass('public.risiko_opd')")) {
                assertTrue(tables.next());
                assertNull(tables.getString(1));
                assertEquals("risiko_opd", tables.getString(2));
            }

            try (ResultSet rows = statement.executeQuery("""
                  SELECT id, kode_risiko, pernyataan_risiko, created_at, updated_at
                  FROM risiko_opd
                  ORDER BY id
                  """)) {
                assertTrue(rows.next());
                assertEquals(27L, rows.getLong("id"));
                assertEquals("RSK-OPD-0027", rows.getString("kode_risiko"));
                assertEquals("Risiko lama", rows.getString("pernyataan_risiko"));
                assertEquals(createdAt, rows.getTimestamp("created_at").toLocalDateTime());
                assertEquals(updatedAt, rows.getTimestamp("updated_at").toLocalDateTime());

                assertTrue(rows.next());
                assertEquals(10_000L, rows.getLong("id"));
                assertEquals("RSK-OPD-10000", rows.getString("kode_risiko"));
                assertFalse(rows.next());
            }

            try (ResultSet objects = statement.executeQuery("""
                  SELECT
                      to_regclass('public.risiko_opd_id_seq') IS NOT NULL AS sequence_exists,
                      to_regclass('public.idx_risiko_opd_sasaran_id') IS NOT NULL AS index_exists,
                      EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'risiko_opd_pkey') AS pk_exists,
                      EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_risiko_opd_kode_risiko') AS uk_exists,
                      EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_risiko_opd_kejadian_consistent') AS ck_exists
                  """)) {
                assertTrue(objects.next());
                assertTrue(objects.getBoolean("sequence_exists"));
                assertTrue(objects.getBoolean("index_exists"));
                assertTrue(objects.getBoolean("pk_exists"));
                assertTrue(objects.getBoolean("uk_exists"));
                assertTrue(objects.getBoolean("ck_exists"));
            }

            try (ResultSet inserted = statement.executeQuery("""
                  INSERT INTO risiko_opd (kode_opd, tahun, kode_sasaran_opd)
                  VALUES ('OPD-001', 2026, 'SAS-003')
                  RETURNING id
                  """)) {
                assertTrue(inserted.next());
                assertEquals(10_001L, inserted.getLong("id"));
            }
        }
    }

    @Test
    void rejectsNonCanonicalLegacyCodeWithoutRenamingTheTable() throws Exception {
        String schema = "legacy_anomaly";
        flywayAt(MigrationVersion.fromVersion("11"), schema).migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                  INSERT INTO legacy_anomaly.risiko (id, kode_risiko)
                  VALUES (7, 'CUSTOM-007')
                  """);
        }

        assertThrows(FlywayException.class, () -> flywayAt(null, schema).migrate());

        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet tables = statement.executeQuery("""
                   SELECT
                       to_regclass('legacy_anomaly.risiko'),
                       to_regclass('legacy_anomaly.risiko_opd')
                   """)) {
            assertTrue(tables.next());
            assertEquals("legacy_anomaly.risiko", tables.getString(1));
            assertNull(tables.getString(2));
        }
    }

    private Flyway flywayAt(MigrationVersion target) {
        return flywayAt(target, "public");
    }

    private Flyway flywayAt(MigrationVersion target, String schema) {
        var configuration = Flyway.configure()
              .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
              .defaultSchema(schema)
              .schemas(schema);
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
              POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
