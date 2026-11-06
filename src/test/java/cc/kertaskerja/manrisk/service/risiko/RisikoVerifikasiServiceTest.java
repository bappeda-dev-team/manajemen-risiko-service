package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.dto.review.ReviewDecisionRequest;
import cc.kertaskerja.manrisk.dto.review.VerifikatorRequest;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.entity.VerifikasiRisiko;
import cc.kertaskerja.manrisk.exception.RiskException;
import cc.kertaskerja.manrisk.repository.RisikoOpdRepository;
import cc.kertaskerja.manrisk.repository.RisikoOperasionalRepository;
import cc.kertaskerja.manrisk.repository.RisikoPemdaRepository;
import cc.kertaskerja.manrisk.repository.VerifikasiRisikoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RisikoVerifikasiServiceTest {
    @Mock private RisikoOperasionalRepository operasionalRepository;
    @Mock private RisikoOpdRepository opdRepository;
    @Mock private RisikoPemdaRepository pemdaRepository;
    @Mock private VerifikasiRisikoRepository verifikasiRepository;
    @InjectMocks private RisikoVerifikasiService service;

    @Test
    void rejectStoresReviewerAndSingleNote() {
        VerifikasiRisiko verification = VerifikasiRisiko.builder()
              .id(42L).kodeRisiko("RSK-OPD-0102").status("SUBMITTED").risikoOpd(risk()).build();
        when(verifikasiRepository.findLockedByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(verification));

        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setStatus("REJECTED");
        request.setCatatan("Perjelas penyebab risiko.");
        request.setVerifikator(verifikator());

        var result = service.decide("RSK-OPD-0102", request);

        assertEquals("REJECTED", result.get("status"));
        assertEquals("U-BUDI", verification.getVerifikatorId());
        assertEquals("Budi Santoso", verification.getVerifikatorNama());
        assertEquals("Perjelas penyebab risiko.", verification.getCatatan());
        verify(verifikasiRepository).save(verification);
    }

    @Test
    void rejectRequiresNote() {
        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setStatus("REJECTED");
        request.setVerifikator(verifikator());

        RiskException error = assertThrows(RiskException.class,
              () -> service.decide("RSK-OPD-0102", request));

        assertEquals(422, error.getStatus());
        assertEquals("RISK_REVIEW_INVALID", error.getCode());
    }

    @Test
    void decisionAcceptsVerifierProvidedByFrontend() {
        VerifikasiRisiko verification = VerifikasiRisiko.builder()
              .id(42L).kodeRisiko("RSK-OPD-0102").status("SUBMITTED").risikoOpd(risk()).build();
        when(verifikasiRepository.findLockedByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(verification));
        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setStatus("VERIFIED");
        request.setVerifikator(verifikator());

        var result = service.decide("RSK-OPD-0102", request);

        assertEquals("VERIFIED", result.get("status"));
        assertEquals("U-BUDI", verification.getVerifikatorId());
    }

    private RisikoOpd risk() {
        return RisikoOpd.builder().id(102L).kodeRisiko("RSK-OPD-0102").kodeOpd("OPD-001")
              .createdBy("U-ANISA").createdName("Anisa").tahun(2026)
              .pernyataanRisiko("Risiko data tidak lengkap").build();
    }

    private VerifikatorRequest verifikator() {
        VerifikatorRequest value = new VerifikatorRequest();
        value.setId("U-BUDI");
        value.setNama("Budi Santoso");
        return value;
    }
}
