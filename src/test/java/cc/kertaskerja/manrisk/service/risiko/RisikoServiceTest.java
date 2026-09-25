package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.entity.Risiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisikoServiceTest {

    private static final String KODE_SASARAN = "SAS-001";

    @Mock
    private RisikoRepository risikoRepository;

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
    void exposesExistingControlForEveryTabAndOccurrenceForNonIdentificationTabs() {
        Risiko record = risiko(2L, "RSK-0002");
        record.setPengendalianYangSudahAda("SOP layanan tersedia untuk diverifikasi.");
        record.setRisikoTerjadi(true);
        record.setWaktuTerjadi(LocalDate.of(2026, 9, 25));
        when(risikoRepository.findByKodeSasaranOpd(KODE_SASARAN)).thenReturn(List.of(record));

        for (String type : List.of("identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan")) {
            RisikoResDTO.RisikoItem item = risikoService.getRisikoByKodeSasaranOpd(KODE_SASARAN, type).getRisiko().getFirst();
            assertEquals("SOP layanan tersedia untuk diverifikasi.", item.getPengendalianYangSudahAda());
            if (!type.equals("identifikasi")) {
                assertEquals(true, item.getRisikoTerjadi());
                assertEquals(LocalDate.of(2026, 9, 25), item.getWaktuTerjadi());
            }
        }
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

    @Test
    void createsRiskWithoutAnExternalSasaranLookup() {
        when(risikoRepository.saveAndFlush(any(Risiko.class))).thenAnswer(invocation -> {
            Risiko saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(risikoRepository.save(any(Risiko.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RisikoResDTO response = risikoService.createRisiko(request("OPD-001", 2026, KODE_SASARAN));

        assertEquals(42L, response.getId());
        assertEquals("RSK-0042", response.getKodeRisiko());
        verify(risikoRepository).saveAndFlush(any(Risiko.class));
    }

    @Test
    void rejectsChangingRiskReferenceDuringUpdate() {
        when(risikoRepository.findById(2L)).thenReturn(java.util.Optional.of(risiko(2L, "RSK-0002")));

        RiskException error = assertThrows(RiskException.class,
              () -> risikoService.updateRisiko(2L, request("OPD-002", 2026, KODE_SASARAN)));

        assertEquals(409, error.getStatus());
        assertEquals("RISK_REFERENCE_IMMUTABLE", error.getCode());
    }

    @Test
    void updatesExistingControlAndOccurrenceFields() {
        Risiko existing = risiko(2L, "RSK-0002");
        when(risikoRepository.findById(2L)).thenReturn(java.util.Optional.of(existing));
        when(risikoRepository.save(existing)).thenReturn(existing);
        RisikoReqDTO request = request("OPD-001", 2026, KODE_SASARAN);
        request.setPengendalianYangSudahAda("  SOP layanan tersedia.  ");
        request.setRisikoTerjadi(true);
        request.setWaktuTerjadi(java.time.LocalDate.of(2026, 9, 25));

        RisikoResDTO result = risikoService.updateRisiko(2L, request);

        assertEquals("SOP layanan tersedia.", result.getPengendalianYangSudahAda());
        assertEquals(true, result.getRisikoTerjadi());
        assertEquals(java.time.LocalDate.of(2026, 9, 25), result.getWaktuTerjadi());
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

    private RisikoReqDTO request(String kodeOpd, Integer tahun, String kodeSasaran) {
        return RisikoReqDTO.builder()
              .kodeOpd(kodeOpd)
              .tahun(tahun)
              .kodeSasaranOpd(kodeSasaran)
              .pernyataanRisiko("Pernyataan")
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .rencanaTindakPengendalian("RTP")
              .kodePerangkatYangMenangani("OPD-001")
              .build();
    }
}
