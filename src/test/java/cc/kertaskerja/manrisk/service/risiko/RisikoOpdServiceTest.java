package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOpdRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisikoOpdServiceTest {

    private static final String KODE_SASARAN = "SAS-001";

    @Mock
    private RisikoOpdRepository risikoOpdRepository;

    @InjectMocks
    private RisikoOpdService risikoOpdService;

    @Test
    void laporanAllSasaranUsesOpdAndYearAndReturnsFullFlatRows() {
        RisikoOpd record = risiko(7L, "RSK-OPD-0007");
        record.setRencanaTindakPengendalian("Evaluasi triwulanan");
        record.setRealisasiTindakPengendalian("Sudah dimulai");
        when(risikoOpdRepository.findByKodeOpdAndTahunOrderByKodeSasaranOpdAscIdAsc("OPD-001", 2026))
              .thenReturn(List.of(record));

        List<RisikoOpdResDTO> response = risikoOpdService.getRisikoLaporan(2026, " OPD-001 ", null);

        assertEquals(1, response.size());
        assertEquals("opd", response.getFirst().getScope());
        assertEquals(2026, response.getFirst().getTahun());
        assertEquals("OPD-001", response.getFirst().getKodeOpd());
        assertEquals(KODE_SASARAN, response.getFirst().getKodeSasaranOpd());
        assertEquals("Evaluasi triwulanan", response.getFirst().getRencanaTindakPengendalian());
        assertEquals("Sudah dimulai", response.getFirst().getRealisasiTindakPengendalian());
        verify(risikoOpdRepository).findByKodeOpdAndTahunOrderByKodeSasaranOpdAscIdAsc("OPD-001", 2026);
    }

    @Test
    void laporanSelectedSasaranUsesOneFilteredQueryAndDeduplicatesCodes() {
        List<String> selected = List.of("SAS-001", "SAS-003");
        when(risikoOpdRepository.findByKodeOpdAndTahunAndKodeSasaranOpdInOrderByKodeSasaranOpdAscIdAsc(
              "OPD-001", 2026, selected)).thenReturn(List.of(risiko(7L, "RSK-OPD-0007")));

        List<RisikoOpdResDTO> response = risikoOpdService.getRisikoLaporan(
              2026, "OPD-001", List.of(" SAS-001 ", "SAS-003", "SAS-001"));

        assertEquals(1, response.size());
        verify(risikoOpdRepository).findByKodeOpdAndTahunAndKodeSasaranOpdInOrderByKodeSasaranOpdAscIdAsc(
              "OPD-001", 2026, selected);
    }

    @Test
    void laporanSelectedSasaranWithoutRisksReturnsEmptyList() {
        when(risikoOpdRepository.findByKodeOpdAndTahunAndKodeSasaranOpdInOrderByKodeSasaranOpdAscIdAsc(
              "OPD-001", 2026, List.of("SAS-EMPTY"))).thenReturn(List.of());

        assertEquals(List.of(), risikoOpdService.getRisikoLaporan(2026, "OPD-001", List.of("SAS-EMPTY")));
    }

    @Test
    void laporanRejectsInvalidContextOrExplicitEmptySasaranBeforeQuery() {
        for (Runnable action : List.<Runnable>of(
              () -> risikoOpdService.getRisikoLaporan(0, "OPD-001", null),
              () -> risikoOpdService.getRisikoLaporan(2026, " ", null),
              () -> risikoOpdService.getRisikoLaporan(2026, "OPD-001", List.of()),
              () -> risikoOpdService.getRisikoLaporan(2026, "OPD-001", List.of("")),
              () -> risikoOpdService.getRisikoLaporan(2026, "OPD-001", List.of("SAS-1,SAS-2")))) {
            RiskException error = assertThrows(RiskException.class, action::run);
            assertEquals(400, error.getStatus());
            assertEquals("RISK_INVALID_INPUT", error.getCode());
        }
        verifyNoInteractions(risikoOpdRepository);
    }

    @Test
    void returnsEachRiskIdentityForIdentifikasiRows() {
        when(risikoOpdRepository.findByKodeSasaranOpdOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of(
              risiko(2L, "RSK-OPD-0002"),
              risiko(7L, "RSK-OPD-0007"),
              risiko(9L, "RSK-OPD-0009")
        ));

        RisikoOpdResDTO response = risikoOpdService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "identifikasi");

        assertEquals(3, response.getRisiko().size());
        assertEquals("opd", response.getScope());
        assertItemIdentity(response.getRisiko().get(0), 2L, "RSK-OPD-0002", "identifikasi");
        assertItemIdentity(response.getRisiko().get(1), 7L, "RSK-OPD-0007", "identifikasi");
        assertItemIdentity(response.getRisiko().get(2), 9L, "RSK-OPD-0009", "identifikasi");
    }

    @Test
    void returnsEachRiskIdentityForNonIdentifikasiRows() {
        when(risikoOpdRepository.findByKodeSasaranOpdOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of(
              risiko(2L, "RSK-OPD-0002"),
              risiko(7L, "RSK-OPD-0007")
        ));

        RisikoOpdResDTO response = risikoOpdService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "analisis");

        assertItemIdentity(response.getRisiko().get(0), 2L, "RSK-OPD-0002", "analisis");
        assertItemIdentity(response.getRisiko().get(1), 7L, "RSK-OPD-0007", "analisis");
        assertNotNull(response.getRisiko().get(0).getSkalaKemungkinan());
    }

    @Test
    void exposesExistingControlForEveryTabAndOccurrenceForNonIdentificationTabs() {
        RisikoOpd record = risiko(2L, "RSK-OPD-0002");
        record.setPengendalianYangSudahAda("SOP layanan tersedia untuk diverifikasi.");
        record.setRisikoTerjadi(true);
        record.setWaktuTerjadi(LocalDate.of(2026, 9, 25));
        when(risikoOpdRepository.findByKodeSasaranOpdOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of(record));

        for (String type : List.of("identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan")) {
            RisikoOpdResDTO.RisikoItem item = risikoOpdService
                  .getRisikoByKodeSasaranOpd(KODE_SASARAN, type).getRisiko().getFirst();
            assertEquals("SOP layanan tersedia untuk diverifikasi.", item.getPengendalianYangSudahAda());
            if (!type.equals("identifikasi")) {
                assertEquals(true, item.getRisikoTerjadi());
                assertEquals(LocalDate.of(2026, 9, 25), item.getWaktuTerjadi());
            }
        }
    }

    @Test
    void returnsAnEmptyRiskListWhenSasaranHasNoRisks() {
        when(risikoOpdRepository.findByKodeSasaranOpdOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of());

        RisikoOpdResDTO response = risikoOpdService
              .getRisikoByKodeSasaranOpd(KODE_SASARAN, "identifikasi");

        assertEquals("opd", response.getScope());
        assertEquals(KODE_SASARAN, response.getKodeSasaranOpd());
        assertEquals(List.of(), response.getRisiko());
    }

    @Test
    void serializesRiskItemIdentityWithApiFieldNames() throws Exception {
        RisikoOpdResDTO response = RisikoOpdResDTO.builder()
              .risiko(List.of(RisikoOpdResDTO.RisikoItem.builder()
                    .id(7L)
                    .kodeRisiko("RSK-OPD-0007")
                    .type("identifikasi")
                    .build()))
              .build();

        JsonNode item = new ObjectMapper()
              .readTree(new ObjectMapper().writeValueAsString(response))
              .path("risiko")
              .get(0);

        assertEquals(7L, item.path("id").asLong());
        assertEquals("RSK-OPD-0007", item.path("kode_risiko").asText());
    }

    @Test
    void createsRiskWithoutAnExternalSasaranLookup() {
        when(risikoOpdRepository.saveAndFlush(any(RisikoOpd.class))).thenAnswer(invocation -> {
            RisikoOpd saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(risikoOpdRepository.save(any(RisikoOpd.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

        RisikoOpdResDTO response = risikoOpdService
              .createRisiko(request("OPD-001", 2026, KODE_SASARAN));

        assertEquals(42L, response.getId());
        assertEquals("opd", response.getScope());
        assertEquals("RSK-OPD-0042", response.getKodeRisiko());
        verify(risikoOpdRepository).saveAndFlush(any(RisikoOpd.class));
    }

    @Test
    void keepsTheFullIdWhenItExceedsFourDigits() {
        when(risikoOpdRepository.saveAndFlush(any(RisikoOpd.class))).thenAnswer(invocation -> {
            RisikoOpd saved = invocation.getArgument(0);
            saved.setId(10_000L);
            return saved;
        });
        when(risikoOpdRepository.save(any(RisikoOpd.class)))
              .thenAnswer(invocation -> invocation.getArgument(0));

        RisikoOpdResDTO response = risikoOpdService
              .createRisiko(request("OPD-001", 2026, KODE_SASARAN));

        assertEquals("RSK-OPD-10000", response.getKodeRisiko());
    }

    @Test
    void acceptsLegacyCodeForReadCompatibility() {
        when(risikoOpdRepository.findByKodeRisiko("RSK-OPD-0007"))
              .thenReturn(java.util.Optional.of(risiko(7L, "RSK-OPD-0007")));

        RisikoOpdResDTO response = risikoOpdService.getRisikoByKodeRisiko("RSK-0007");

        assertEquals("RSK-OPD-0007", response.getKodeRisiko());
        verify(risikoOpdRepository).findByKodeRisiko("RSK-OPD-0007");
    }

    @Test
    void rejectsUnknownTabType() {
        RiskException error = assertThrows(RiskException.class,
              () -> risikoOpdService.getRisikoByKodeSasaranOpd(KODE_SASARAN, "unknown"));

        assertEquals(400, error.getStatus());
        assertEquals("RISK_TYPE_INVALID", error.getCode());
    }

    @Test
    void rejectsChangingRiskReferenceDuringUpdate() {
        when(risikoOpdRepository.findById(2L))
              .thenReturn(java.util.Optional.of(risiko(2L, "RSK-OPD-0002")));

        RiskException error = assertThrows(RiskException.class,
              () -> risikoOpdService.updateRisiko(2L, request("OPD-002", 2026, KODE_SASARAN)));

        assertEquals(409, error.getStatus());
        assertEquals("RISK_REFERENCE_IMMUTABLE", error.getCode());
    }

    @Test
    void updatesExistingControlAndOccurrenceFields() {
        RisikoOpd existing = risiko(2L, "RSK-OPD-0002");
        when(risikoOpdRepository.findById(2L)).thenReturn(java.util.Optional.of(existing));
        when(risikoOpdRepository.save(existing)).thenReturn(existing);
        RisikoOpdReqDTO request = request("OPD-001", 2026, KODE_SASARAN);
        request.setPengendalianYangSudahAda("  SOP layanan tersedia.  ");
        request.setRisikoTerjadi(true);
        request.setWaktuTerjadi(java.time.LocalDate.of(2026, 9, 25));

        RisikoOpdResDTO result = risikoOpdService.updateRisiko(2L, request);

        assertEquals("SOP layanan tersedia.", result.getPengendalianYangSudahAda());
        assertEquals(true, result.getRisikoTerjadi());
        assertEquals(java.time.LocalDate.of(2026, 9, 25), result.getWaktuTerjadi());
    }

    private void assertItemIdentity(RisikoOpdResDTO.RisikoItem item, Long id,
                                    String kodeRisiko, String type) {
        assertEquals(id, item.getId());
        assertEquals(kodeRisiko, item.getKodeRisiko());
        assertEquals(type, item.getType());
    }

    private RisikoOpd risiko(Long id, String kodeRisiko) {
        return RisikoOpd.builder()
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

    private RisikoOpdReqDTO request(String kodeOpd, Integer tahun, String kodeSasaran) {
        return RisikoOpdReqDTO.builder()
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
