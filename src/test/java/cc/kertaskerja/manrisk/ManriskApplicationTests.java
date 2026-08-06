package cc.kertaskerja.manrisk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestContainersConfiguration.class)
@SpringBootTest()
@ActiveProfiles("test")
class ManriskApplicationTests {
	@Test
	void contextLoads() {
	}

}
