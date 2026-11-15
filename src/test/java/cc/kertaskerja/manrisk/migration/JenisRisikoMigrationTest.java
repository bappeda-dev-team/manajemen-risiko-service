package cc.kertaskerja.manrisk.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class JenisRisikoMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void seedsSevenCategoriesAndRejectsDuplicateNames() throws Exception {
        Flyway.configure()
              .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
              .locations("classpath:db/migration")
              .load()
              .migrate();

        try (var connection = DriverManager.getConnection(
              POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT id, nama FROM jenis_risiko ORDER BY id")) {
                int count = 0;
                while (rows.next()) {
                    count++;
                    if (count == 1) assertEquals("Risiko Strategis", rows.getString("nama"));
                }
                assertEquals(7, count);
            }

            SQLException duplicate = assertThrows(SQLException.class,
                  () -> statement.executeUpdate("INSERT INTO jenis_risiko (nama) VALUES (' risiko strategis ')"));
            assertEquals("23505", duplicate.getSQLState());

            try (ResultSet inserted = statement.executeQuery(
                  "INSERT INTO jenis_risiko (nama) VALUES ('Risiko Baru') RETURNING id")) {
                assertTrue(inserted.next());
                assertEquals(8L, inserted.getLong("id"));
            }
        }
    }
}
