package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.config.JacksonConfig;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaResDTO;
import cc.kertaskerja.manrisk.exception.GlobalExceptionHandler;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.service.risiko.RisikoPemdaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RisikoPemdaControllerTest {
    @Mock
    private RisikoPemdaService service;

    @InjectMocks
    private RisikoPemdaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
              .setControllerAdvice(new GlobalExceptionHandler())
              .setMessageConverters(new MappingJackson2HttpMessageConverter(
                    new JacksonConfig().objectMapper()))
              .build();
    }

    @Test
    void validCreateReturnsCreated() throws Exception {
        when(service.createRisiko(any())).thenReturn(RisikoPemdaResDTO.builder()
              .scope("pemda").id(1L).kodeRisiko("RSK-PEM-0001").build());

        mockMvc.perform(post("/risiko-pemda")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest()))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.data.scope").value("pemda"))
              .andExpect(jsonPath("$.data.kode_risiko").value("RSK-PEM-0001"));
    }

    @Test
    void missingPemdaSasaranAndInvalidScaleReturnBadRequest() throws Exception {
        String request = validRequest()
              .replace("\"kode_sasaran_pemda\":\"SAS-PEM-001\",", "")
              .replace("\"skala_kemungkinan\":3", "\"skala_kemungkinan\":8");

        mockMvc.perform(post("/risiko-pemda")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.data.code").value("RISK_INVALID_INPUT"));

        verifyNoInteractions(service);
    }

    @Test
    void rejectsOpdReferenceFieldsAsUnknownProperties() throws Exception {
        String request = validRequest().replace("{", "{\"kode_opd\":\"OPD-001\",");

        mockMvc.perform(post("/risiko-pemda")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.data.code").value("RISK_INVALID_INPUT"));

        verifyNoInteractions(service);
    }

    @Test
    void unknownTabTypeReturnsStableBadRequest() throws Exception {
        when(service.getRisikoByKodeSasaranPemda(eq("SAS-PEM-001"), eq("unknown")))
              .thenThrow(new RiskException(400, "RISK_TYPE_INVALID", "Tipe tab risiko tidak dikenali."));

        mockMvc.perform(get("/risiko-pemda/sasaran/SAS-PEM-001").param("type", "unknown"))
              .andExpect(status().isBadRequest())
              .andExpect(jsonPath("$.data.code").value("RISK_TYPE_INVALID"));
    }

    private String validRequest() {
        return """
              {"tahun":2026,"kode_sasaran_pemda":"SAS-PEM-001",
               "pernyataan_risiko":"Pernyataan","skala_kemungkinan":3,"skala_dampak":4,
               "rencana_tindak_pengendalian":"RTP","kode_perangkat_yang_menangani":"OPD-001"}
              """;
    }
}
