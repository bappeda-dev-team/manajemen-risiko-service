package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.entity.Risiko;
import cc.kertaskerja.manrisk.repository.RisikoRepository;
import cc.kertaskerja.manrisk.service.risiko.external.ExternalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisikoServiceTest {

    private static final String KODE_SASARAN = "SAS-001";

    @Mock
    private RisikoRepository risikoRepository;

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private RisikoService risikoService;

    @Test
    void returnsEachRiskIdentityForIdentifikasiRows() {
        when(risikoRepository.findByKodeSasaranOpd(KODE_SASARAN)).thenReturn(List.of(
              risiko(9L, "RSK-0009"),
              risiko(2L, "RSK-0002"),
              risiko(7L, "RSK-0007")
        ));

        RisikoResDTO response = risikoService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "identifikasi");

        assertEquals(3, response.getRisiko().size());
        assertItemIdentity(response.getRisiko().get(0), 2L, "RSK-0002", "identifikasi");
        assertItemIdentity(response.getRisiko().get(1), 7L, "RSK-0007", "identifikasi");
        assertItemIdentity(response.getRisiko().get(2), 9L, "RSK-0009", "identifikasi");
    }

    @Test
    void returnsEachRiskIdentityForNonIdentifikasiRows() {
        when(risikoRepository.findByKodeSasaranOpd(KODE_SASARAN)).thenReturn(List.of(
              risiko(2L, "RSK-0002"),
              risiko(7L, "RSK-0007")
        ));

        RisikoResDTO response = risikoService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "analisis");

        assertItemIdentity(response.getRisiko().get(0), 2L, "RSK-0002", "analisis");
        assertItemIdentity(response.getRisiko().get(1), 7L, "RSK-0007", "analisis");
        assertNotNull(response.getRisiko().get(0).getSkalaKemungkinan());
    }

    @Test
    void returnsAnEmptyRiskListWhenSasaranHasNoRisks() {
        when(risikoRepository.findByKodeSasaranOpd(KODE_SASARAN)).thenReturn(List.of());

        RisikoResDTO response = risikoService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "identifikasi");

        assertEquals(KODE_SASARAN, response.getKodeSasaranOpd());
        assertEquals(List.of(), response.getRisiko());
    }

    @Test
    void serializesRiskItemIdentityWithApiFieldNames() throws Exception {
        RisikoResDTO response = RisikoResDTO.builder()
              .risiko(List.of(RisikoResDTO.RisikoItem.builder()
                    .id(7L)
                    .kodeRisiko("RSK-0007")
                    .type("identifikasi")
                    .build()))
              .build();

        JsonNode item = new ObjectMapper()
              .readTree(new ObjectMapper().writeValueAsString(response))
              .path("risiko")
              .get(0);

        assertEquals(7L, item.path("id").asLong());
        assertEquals("RSK-0007", item.path("kode_risiko").asText());
    }

    private void assertItemIdentity(RisikoResDTO.RisikoItem item, Long id, String kodeRisiko, String type) {
        assertEquals(id, item.getId());
        assertEquals(kodeRisiko, item.getKodeRisiko());
        assertEquals(type, item.getType());
    }

    private Risiko risiko(Long id, String kodeRisiko) {
        return Risiko.builder()
              .id(id)
              .kodeOpd("OPD-001")
              .kodeRisiko(kodeRisiko)
              .tahun(2026)
              .kodeSasaranOpd(KODE_SASARAN)
              .permasalahan("Permasalahan " + id)
              .sebabPermasalahan("Sebab " + id)
              .pernyataanRisiko("Pernyataan " + id)
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .build();
    }
}
