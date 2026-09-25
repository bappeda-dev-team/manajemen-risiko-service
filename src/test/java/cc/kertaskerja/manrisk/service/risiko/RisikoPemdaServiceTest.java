package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaResDTO;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoPemdaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RisikoPemdaServiceTest {
    private static final String KODE_SASARAN = "SAS-PEM-001";

    @Mock
    private RisikoPemdaRepository repository;

    @InjectMocks
    private RisikoPemdaService service;

    @Test
    void emptySasaranReturnsScopedWrapperWithEmptyList() {
        when(repository.findByKodeSasaranPemdaOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of());

        RisikoPemdaResDTO result = service.getRisikoByKodeSasaranPemda(KODE_SASARAN, "identifikasi");

        assertEquals("pemda", result.getScope());
        assertEquals(KODE_SASARAN, result.getKodeSasaranPemda());
        assertEquals(List.of(), result.getRisiko());
    }

    @Test
    void allTabsKeepTheSameRiskIdentity() {
        when(repository.findByKodeSasaranPemdaOrderByIdAsc(KODE_SASARAN))
              .thenReturn(List.of(risiko(7L, "RSK-PEM-0007")));

        for (String type : List.of("identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan")) {
            RisikoPemdaResDTO.RisikoItem item = service
                  .getRisikoByKodeSasaranPemda(KODE_SASARAN, type).getRisiko().get(0);
            assertEquals(7L, item.getId());
            assertEquals("RSK-PEM-0007", item.getKodeRisiko());
            assertEquals(type, item.getType());
        }
    }

    @Test
    void exposesExistingControlForEveryTabAndOccurrenceForNonIdentificationTabs() {
        RisikoPemda record = risiko(7L, "RSK-PEM-0007");
        record.setPengendalianYangSudahAda("SOP layanan tersedia untuk diverifikasi.");
        record.setRisikoTerjadi(true);
        record.setWaktuTerjadi(LocalDate.of(2026, 9, 25));
        when(repository.findByKodeSasaranPemdaOrderByIdAsc(KODE_SASARAN)).thenReturn(List.of(record));

        for (String type : List.of("identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan")) {
            RisikoPemdaResDTO.RisikoItem item = service.getRisikoByKodeSasaranPemda(KODE_SASARAN, type).getRisiko().getFirst();
            assertEquals("SOP layanan tersedia untuk diverifikasi.", item.getPengendalianYangSudahAda());
            if (!type.equals("identifikasi")) {
                assertEquals(true, item.getRisikoTerjadi());
                assertEquals(LocalDate.of(2026, 9, 25), item.getWaktuTerjadi());
            }
        }
    }

    @Test
    void rejectsUnknownTabTypeBeforeQueryingRepository() {
        RiskException error = assertThrows(RiskException.class,
              () -> service.getRisikoByKodeSasaranPemda(KODE_SASARAN, "unknown"));

        assertEquals(400, error.getStatus());
        assertEquals("RISK_TYPE_INVALID", error.getCode());
        verifyNoInteractions(repository);
    }

    @Test
    void consecutiveCreatesUseDatabaseIdsForUniqueCodes() {
        AtomicLong sequence = new AtomicLong(40);
        when(repository.saveAndFlush(any(RisikoPemda.class))).thenAnswer(invocation -> {
            RisikoPemda record = invocation.getArgument(0);
            record.setId(sequence.incrementAndGet());
            return record;
        });
        when(repository.save(any(RisikoPemda.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RisikoPemdaResDTO first = service.createRisiko(request(KODE_SASARAN, 2026));
        RisikoPemdaResDTO second = service.createRisiko(request(KODE_SASARAN, 2026));

        assertEquals("RSK-PEM-0041", first.getKodeRisiko());
        assertEquals("RSK-PEM-0042", second.getKodeRisiko());
        assertNotEquals(first.getKodeRisiko(), second.getKodeRisiko());
    }

    @Test
    void updateNormalizesMutableFieldsAndKeepsReference() {
        RisikoPemda existing = risiko(2L, "RSK-PEM-0002");
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        RisikoPemdaReqDTO request = request(KODE_SASARAN, 2026);
        request.setPermasalahan("  Masalah baru  ");
        request.setPengendalianYangSudahAda("  SOP layanan tersedia.  ");
        request.setRisikoTerjadi(true);
        request.setWaktuTerjadi(LocalDate.of(2026, 9, 25));

        RisikoPemdaResDTO result = service.updateRisiko(2L, request);

        assertEquals("Masalah baru", result.getPermasalahan());
        assertEquals(KODE_SASARAN, result.getKodeSasaranPemda());
        assertEquals("RSK-PEM-0002", result.getKodeRisiko());
        assertEquals("SOP layanan tersedia.", result.getPengendalianYangSudahAda());
        assertEquals(true, result.getRisikoTerjadi());
        assertEquals(LocalDate.of(2026, 9, 25), result.getWaktuTerjadi());
    }

    @Test
    void updateRejectsChangingPemdaReference() {
        when(repository.findById(2L)).thenReturn(Optional.of(risiko(2L, "RSK-PEM-0002")));

        RiskException error = assertThrows(RiskException.class,
              () -> service.updateRisiko(2L, request("SAS-PEM-999", 2026)));

        assertEquals(409, error.getStatus());
        assertEquals("RISK_REFERENCE_IMMUTABLE", error.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void missingDetailAndDeleteReturnStableNotFoundError() {
        when(repository.findByKodeRisiko("RSK-PEM-9999")).thenReturn(Optional.empty());
        when(repository.findById(9999L)).thenReturn(Optional.empty());

        RiskException detail = assertThrows(RiskException.class,
              () -> service.getRisikoByKodeRisiko("RSK-PEM-9999"));
        RiskException delete = assertThrows(RiskException.class, () -> service.deleteRisiko(9999L));

        assertEquals("RISK_NOT_FOUND", detail.getCode());
        assertEquals("RISK_NOT_FOUND", delete.getCode());
    }

    @Test
    void deleteUsesPemdaRepositoryRecordIdentity() {
        RisikoPemda existing = risiko(3L, "RSK-PEM-0003");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        service.deleteRisiko(3L);

        verify(repository).delete(existing);
    }

    private RisikoPemda risiko(Long id, String kodeRisiko) {
        return RisikoPemda.builder()
              .id(id)
              .kodeRisiko(kodeRisiko)
              .tahun(2026)
              .kodeSasaranPemda(KODE_SASARAN)
              .permasalahan("Masalah")
              .sebabPermasalahan("Sebab")
              .pernyataanRisiko("Pernyataan")
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .rencanaTindakPengendalian("RTP")
              .kodePerangkatYangMenangani("OPD-001")
              .build();
    }

    private RisikoPemdaReqDTO request(String kodeSasaran, Integer tahun) {
        return RisikoPemdaReqDTO.builder()
              .tahun(tahun)
              .kodeSasaranPemda(kodeSasaran)
              .pernyataanRisiko("Pernyataan")
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .rencanaTindakPengendalian("RTP")
              .kodePerangkatYangMenangani("OPD-001")
              .build();
    }
}
