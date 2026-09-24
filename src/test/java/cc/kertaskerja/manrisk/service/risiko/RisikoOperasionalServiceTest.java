package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalResDTO;
import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOperasionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisikoOperasionalServiceTest {
    private static final String KODE_REKIN = "PK-2026-001";

    @Mock
    private RisikoOperasionalRepository repository;

    @InjectMocks
    private RisikoOperasionalService service;

    @Test
    void emptyRekinReturnsScopedWrapperWithEmptyList() {
        when(repository.findByKodeRekinOrderByIdAsc(KODE_REKIN)).thenReturn(List.of());

        RisikoOperasionalResDTO result = service.getRisikoByKodeRekin(KODE_REKIN, "identifikasi");

        assertEquals("operasional", result.getScope());
        assertEquals(KODE_REKIN, result.getKodeRekin());
        assertEquals(List.of(), result.getRisiko());
    }

    @Test
    void allTabsKeepTheSameRiskIdentity() {
        when(repository.findByKodeRekinOrderByIdAsc(KODE_REKIN))
              .thenReturn(List.of(risiko(7L, "RSK-OPR-0007")));

        for (String type : List.of("identifikasi", "analisis", "pengendalian", "pemantauan", "hasil-pemantauan")) {
            RisikoOperasionalResDTO.RisikoItem item = service.getRisikoByKodeRekin(KODE_REKIN, type)
                  .getRisiko().getFirst();
            assertEquals(7L, item.getId());
            assertEquals("RSK-OPR-0007", item.getKodeRisiko());
            assertEquals(type, item.getType());
        }
    }

    @Test
    void rejectsUnknownTabBeforeQueryingRepository() {
        RiskException error = assertThrows(RiskException.class,
              () -> service.getRisikoByKodeRekin(KODE_REKIN, "lainnya"));

        assertEquals(400, error.getStatus());
        assertEquals("RISK_TYPE_INVALID", error.getCode());
        verifyNoInteractions(repository);
    }

    @Test
    void consecutiveCreatesUseDatabaseIdsForUniqueCodes() {
        AtomicLong sequence = new AtomicLong(40);
        when(repository.saveAndFlush(any(RisikoOperasional.class))).thenAnswer(invocation -> {
            RisikoOperasional record = invocation.getArgument(0);
            record.setId(sequence.incrementAndGet());
            return record;
        });
        when(repository.save(any(RisikoOperasional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RisikoOperasionalResDTO first = service.createRisiko(request());
        RisikoOperasionalResDTO second = service.createRisiko(request());

        assertEquals("RSK-OPR-0041", first.getKodeRisiko());
        assertEquals("RSK-OPR-0042", second.getKodeRisiko());
        assertNotEquals(first.getKodeRisiko(), second.getKodeRisiko());
    }

    @Test
    void updateNormalizesMutableFieldsAndKeepsReference() {
        RisikoOperasional existing = risiko(2L, "RSK-OPR-0002");
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        RisikoOperasionalReqDTO request = request();
        request.setPermasalahan("  Masalah baru  ");

        RisikoOperasionalResDTO result = service.updateRisiko(2L, request);

        assertEquals("Masalah baru", result.getPermasalahan());
        assertEquals(KODE_REKIN, result.getKodeRekin());
        assertEquals("RSK-OPR-0002", result.getKodeRisiko());
    }

    @Test
    void updateRejectsChangingOperasionalReference() {
        when(repository.findById(2L)).thenReturn(Optional.of(risiko(2L, "RSK-OPR-0002")));
        RisikoOperasionalReqDTO request = request();
        request.setPegawaiId("19880001");

        RiskException error = assertThrows(RiskException.class, () -> service.updateRisiko(2L, request));

        assertEquals(409, error.getStatus());
        assertEquals("RISK_REFERENCE_IMMUTABLE", error.getCode());
        verify(repository, never()).save(any());
    }

    @Test
    void missingDetailAndDeleteReturnStableNotFoundError() {
        when(repository.findByKodeRisiko("RSK-OPR-9999")).thenReturn(Optional.empty());
        when(repository.findById(9999L)).thenReturn(Optional.empty());

        RiskException detail = assertThrows(RiskException.class,
              () -> service.getRisikoByKodeRisiko("RSK-OPR-9999"));
        RiskException delete = assertThrows(RiskException.class, () -> service.deleteRisiko(9999L));

        assertEquals("RISK_NOT_FOUND", detail.getCode());
        assertEquals("RISK_NOT_FOUND", delete.getCode());
    }

    private RisikoOperasional risiko(Long id, String kodeRisiko) {
        return RisikoOperasional.builder()
              .id(id)
              .kodeRisiko(kodeRisiko)
              .tahun(2026)
              .kodeRekin(KODE_REKIN)
              .kodeOpd("OPD-001")
              .pegawaiId("19870001")
              .permasalahan("Masalah")
              .sebabPermasalahan("Sebab")
              .pernyataanRisiko("Pernyataan")
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .rencanaTindakPengendalian("RTP")
              .kodePerangkatYangMenangani("OPD-001")
              .build();
    }

    private RisikoOperasionalReqDTO request() {
        return RisikoOperasionalReqDTO.builder()
              .tahun(2026)
              .kodeRekin(KODE_REKIN)
              .kodeOpd("OPD-001")
              .pegawaiId("19870001")
              .pernyataanRisiko("Pernyataan")
              .skalaKemungkinan(3)
              .skalaDampak(4)
              .rencanaTindakPengendalian("RTP")
              .kodePerangkatYangMenangani("OPD-001")
              .build();
    }
}
