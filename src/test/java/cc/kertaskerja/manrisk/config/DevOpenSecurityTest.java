package cc.kertaskerja.manrisk.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DevOpenSecurityTest.ProbeController.class,
      properties = "kertaskerja.cors.enabled=false")
@ContextConfiguration(classes = {SecurityConfig.class, DevOpenSecurityTest.ProbeController.class})
class DevOpenSecurityTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void defaultModeAcceptsRequestWithoutMahuluCookie() throws Exception {
        mockMvc.perform(get("/auth-mode-probe"))
              .andExpect(status().isOk())
              .andExpect(content().string("ok"));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/auth-mode-probe")
        String probe() { return "ok"; }
    }
}
