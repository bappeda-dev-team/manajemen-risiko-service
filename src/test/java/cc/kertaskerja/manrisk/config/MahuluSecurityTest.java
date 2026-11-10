package cc.kertaskerja.manrisk.config;

import cc.kertaskerja.manrisk.security.MahuluSessionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MahuluSecurityTest.ProbeController.class,
      properties = {"manrisk.auth.mode=mahulu", "kertaskerja.cors.enabled=false"})
@ContextConfiguration(classes = {SecurityConfig.class, MahuluSecurityTest.ProbeController.class, MahuluSecurityTest.TestBeans.class})
class MahuluSecurityTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MahuluSessionClient sessionClient;

    @Test
    void rejectsRequestWithoutMahuluCookie() throws Exception {
        mockMvc.perform(get("/auth-mode-probe"))
              .andExpect(status().isUnauthorized())
              .andExpect(jsonPath("$.data.code").value("AUTH_SESSION_INVALID"));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/auth-mode-probe")
        String probe() { return "ok"; }
    }

    @Configuration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() { return new ObjectMapper(); }
    }
}
