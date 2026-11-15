package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.JenisRisikoResponse;
import cc.kertaskerja.manrisk.exception.GlobalExceptionHandler;
import cc.kertaskerja.manrisk.service.risiko.JenisRisikoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JenisRisikoControllerTest {
    @Mock
    private JenisRisikoService service;

    @InjectMocks
    private JenisRisikoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
              .setControllerAdvice(new GlobalExceptionHandler())
              .build();
    }

    @Test
    void listAndDetailReturnData() throws Exception {
        JenisRisikoResponse risk = new JenisRisikoResponse(1L, "Risiko Strategis");
        when(service.findAll()).thenReturn(List.of(risk));
        when(service.findById(1L)).thenReturn(risk);

        mockMvc.perform(get("/jenis-risiko"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data[0].nama").value("Risiko Strategis"));
        mockMvc.perform(get("/jenis-risiko/1"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void createUpdateAndDeleteUseExpectedRoutes() throws Exception {
        when(service.create(any())).thenReturn(new JenisRisikoResponse(8L, "Risiko Baru"));
        when(service.update(any(), any())).thenReturn(new JenisRisikoResponse(8L, "Risiko Lain"));

        mockMvc.perform(post("/jenis-risiko")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nama\":\"Risiko Baru\"}"))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.data.id").value(8));
        mockMvc.perform(put("/jenis-risiko/8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nama\":\"Risiko Lain\"}"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.nama").value("Risiko Lain"));
        mockMvc.perform(delete("/jenis-risiko/8"))
              .andExpect(status().isOk());
        verify(service).delete(8L);
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mockMvc.perform(post("/jenis-risiko")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nama\":\"  \"}"))
              .andExpect(status().isBadRequest());
    }
}
