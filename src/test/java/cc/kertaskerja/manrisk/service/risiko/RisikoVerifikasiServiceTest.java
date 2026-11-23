package cc.kertaskerja.manrisk.service.risiko;

import cc.kertaskerja.manrisk.controller.RisikoVerifikasiController;
import cc.kertaskerja.manrisk.dto.review.ReviewDecisionRequest;
import cc.kertaskerja.manrisk.dto.review.SubmitVerificationRequest;
import cc.kertaskerja.manrisk.dto.review.VerifikatorRequest;
import cc.kertaskerja.manrisk.dto.review.PemohonRequest;
import cc.kertaskerja.manrisk.entity.RisikoOpd;
import cc.kertaskerja.manrisk.entity.RisikoOperasional;
import cc.kertaskerja.manrisk.entity.RisikoPemda;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RisikoVerifikasiServiceTest {
    @Mock private RisikoOperasionalRepository operasionalRepository;
    @Mock private RisikoOpdRepository opdRepository;
    @Mock private RisikoPemdaRepository pemdaRepository;
    @Mock private VerifikasiRisikoRepository verifikasiRepository;
    @InjectMocks private RisikoVerifikasiService service;

    @Test
    void rejectStoresReviewerAndSingleNote() {
        RisikoOpd risk = risk();
        risk.setPerangkatYangMenangani("Dinas Pendidikan");
        risk.setKodePerangkatYangMenangani("OPD-PENDIDIKAN");
        VerifikasiRisiko verification = VerifikasiRisiko.builder()
              .id(42L).kodeRisiko("RSK-OPD-0102").status("SUBMITTED").risikoOpd(risk).build();
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
        assertHandlingFields(result, "Dinas Pendidikan", "OPD-PENDIDIKAN");
        verify(verifikasiRepository).save(verification);
    }

    @Test
    void submitStoresApplicantFromRequest() {
        RisikoOpd risk = risk();
        risk.setSkalaKemungkinan(3);
        risk.setSkalaDampak(4);
        risk.setRencanaTindakPengendalian("Perbaiki data");
        risk.setPerangkatYangMenangani("Dinas Pendidikan");
        risk.setKodePerangkatYangMenangani("OPD-001");
        when(opdRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(risk));
        when(verifikasiRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.empty());

        SubmitVerificationRequest request = new SubmitVerificationRequest();
        request.setKodeRisiko("RSK-OPD-0102");
        PemohonRequest pemohon = new PemohonRequest();
        pemohon.setId("19800001");
        pemohon.setNama("Sinta Arnes");
        request.setPemohon(pemohon);

        var result = service.submit(request);

        assertEquals("19800001", risk.getCreatedBy());
        assertEquals("Sinta Arnes", risk.getCreatedName());
        assertEquals("Sinta Arnes", ((java.util.Map<?, ?>) result.get("pemohon")).get("nama"));
        assertHandlingFields(result, "Dinas Pendidikan", "OPD-001");
        verify(opdRepository).saveAndFlush(risk);
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

    @Test
    void authenticatedSubmitStoresApplicantFromRequestAndReturnsItOnGet() {
        RisikoOpd risk = risk();
        risk.setSkalaKemungkinan(3);
        risk.setSkalaDampak(4);
        risk.setRencanaTindakPengendalian("Perbaiki data");
        risk.setKodePerangkatYangMenangani("OPD-001");
        when(opdRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(risk));
        when(verifikasiRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.empty());
        SubmitVerificationRequest request = new SubmitVerificationRequest();
        request.setKodeRisiko("RSK-OPD-0102");
        request.setPemohon(pemohon(" U-ANISA ", " Sinta Arnes "));

        var result = service.submit(request, principal("U-ANISA", "level_3"));

        assertEquals("U-ANISA", risk.getCreatedBy());
        assertEquals("Sinta Arnes", risk.getCreatedName());
        assertEquals("Sinta Arnes", ((java.util.Map<?, ?>) result.get("pemohon")).get("nama"));
        verify(opdRepository).saveAndFlush(risk);
        verify(verifikasiRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(VerifikasiRisiko.class));

        when(opdRepository.findAll()).thenReturn(List.of(risk));
        when(operasionalRepository.findAll()).thenReturn(List.of());
        when(pemdaRepository.findAll()).thenReturn(List.of());
        var daftar = (List<?>) service.daftarPengajuan().get("daftar_pengajuan");
        assertEquals("Sinta Arnes", ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) daftar.get(0)).get("pemohon")).get("nama"));
    }

    @Test
    void authenticatedSubmitRejectsApplicantFromAnotherAccount() {
        RisikoOpd risk = complete(risk());
        when(opdRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(risk));
        when(verifikasiRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.empty());
        SubmitVerificationRequest request = new SubmitVerificationRequest();
        request.setKodeRisiko("RSK-OPD-0102");
        request.setPemohon(pemohon("U-LAIN", "Pengguna Lain"));

        RiskException error = assertThrows(RiskException.class,
              () -> service.submit(request, principal("U-ANISA", "level_3")));

        assertEquals(422, error.getStatus());
        assertEquals("Anisa", risk.getCreatedName());
        verify(opdRepository, never()).saveAndFlush(risk);
        verify(verifikasiRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticatedSubmitRequiresApplicantBody() {
        RisikoOpd risk = complete(risk());
        when(opdRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(risk));
        when(verifikasiRepository.findByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.empty());
        SubmitVerificationRequest request = new SubmitVerificationRequest();
        request.setKodeRisiko("RSK-OPD-0102");

        RiskException error = assertThrows(RiskException.class,
              () -> service.submit(request, principal("U-ANISA", "level_3")));

        assertEquals(422, error.getStatus());
        verify(verifikasiRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitPersistsApplicantForPemda() {
        RisikoPemda pemda = RisikoPemda.builder().id(3L).kodeRisiko("RSK-PEM-0003").tahun(2026)
              .pernyataanRisiko("Risiko Pemda").skalaKemungkinan(3).skalaDampak(4)
              .rencanaTindakPengendalian("Perbaiki data").kodePerangkatYangMenangani("OPD-001").build();
        when(pemdaRepository.findByKodeRisiko("RSK-PEM-0003")).thenReturn(Optional.of(pemda));
        when(verifikasiRepository.findByKodeRisiko("RSK-PEM-0003")).thenReturn(Optional.empty());

        SubmitVerificationRequest pemdaRequest = new SubmitVerificationRequest();
        pemdaRequest.setKodeRisiko("RSK-PEM-0003");
        pemdaRequest.setPemohon(pemohon("U-ANISA", "Sinta Arnes"));

        assertEquals("Sinta Arnes", ((java.util.Map<?, ?>) service.submit(pemdaRequest).get("pemohon")).get("nama"));
        verify(pemdaRepository).saveAndFlush(pemda);
    }

    @Test
    void submitPersistsApplicantForOperasional() {
        RisikoOperasional operasional = RisikoOperasional.builder().id(4L).kodeRisiko("RSK-OPR-0004").tahun(2026)
              .pernyataanRisiko("Risiko operasional").skalaKemungkinan(3).skalaDampak(4)
              .rencanaTindakPengendalian("Perbaiki data").kodePerangkatYangMenangani("OPD-001").build();
        when(operasionalRepository.findByKodeRisiko("RSK-OPR-0004")).thenReturn(Optional.of(operasional));
        when(verifikasiRepository.findByKodeRisiko("RSK-OPR-0004")).thenReturn(Optional.empty());

        SubmitVerificationRequest operasionalRequest = new SubmitVerificationRequest();
        operasionalRequest.setKodeRisiko("RSK-OPR-0004");
        operasionalRequest.setPemohon(pemohon("U-ANISA", "Sinta Arnes"));

        assertEquals("Sinta Arnes", ((java.util.Map<?, ?>) service.submit(operasionalRequest).get("pemohon")).get("nama"));
        verify(operasionalRepository).saveAndFlush(operasional);
    }

    @Test
    void authenticatedDecisionStoresSessionVerifier() {
        VerifikasiRisiko verification = VerifikasiRisiko.builder()
              .id(42L).kodeRisiko("RSK-OPD-0102").status("SUBMITTED").risikoOpd(risk()).build();
        when(verifikasiRepository.findLockedByKodeRisiko("RSK-OPD-0102")).thenReturn(Optional.of(verification));
        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setStatus("VERIFIED");

        service.decide("RSK-OPD-0102", request, principal("U-BUDI", "level_2"));

        assertEquals("U-BUDI", verification.getVerifikatorId());
        assertEquals("Budi", verification.getVerifikatorNama());
    }

    @Test
    void opdDetailsReturnHandlingNameAndCodes() {
        RisikoOpd risk = risk();
        risk.setPerangkatYangMenangani("Dinas Pendidikan");
        risk.setKodePerangkatYangMenangani("OPD-PENDIDIKAN");
        when(opdRepository.findByKodeRisiko(risk.getKodeRisiko())).thenReturn(Optional.of(risk));

        assertHandlingFieldsInBothDetails(risk.getKodeRisiko(), "Dinas Pendidikan", "OPD-PENDIDIKAN");
    }

    @Test
    void pemdaDetailsReturnHandlingNameAndMultipleCodes() {
        RisikoPemda risk = RisikoPemda.builder().id(3L).kodeRisiko("RSK-PEM-0003").createdBy("U-ANISA").tahun(2026)
              .perangkatYangMenangani("Dinas A, Dinas B").kodePerangkatYangMenangani("OPD-002, OPD-001").build();
        when(pemdaRepository.findByKodeRisiko(risk.getKodeRisiko())).thenReturn(Optional.of(risk));

        assertHandlingFieldsInBothDetails(risk.getKodeRisiko(), "Dinas A, Dinas B", "OPD-002, OPD-001");
    }

    @Test
    void operasionalDetailsReturnHandlingNameAndCodes() {
        RisikoOperasional risk = RisikoOperasional.builder().id(4L).kodeRisiko("RSK-OPR-0004").createdBy("U-ANISA")
              .kodeOpd("OPD-001").tahun(2026).perangkatYangMenangani("Dinas Kesehatan")
              .kodePerangkatYangMenangani("OPD-KESEHATAN").build();
        when(operasionalRepository.findByKodeRisiko(risk.getKodeRisiko())).thenReturn(Optional.of(risk));

        assertHandlingFieldsInBothDetails(risk.getKodeRisiko(), "Dinas Kesehatan", "OPD-KESEHATAN");
    }

    @Test
    void detailsReturnHandlingKeysWhenStoredValuesAreNull() {
        RisikoOpd risk = risk();
        risk.setKodePerangkatYangMenangani("OPD-002, OPD-003");
        when(opdRepository.findByKodeRisiko(risk.getKodeRisiko())).thenReturn(Optional.of(risk));

        assertHandlingFieldsInBothDetails(risk.getKodeRisiko(), null, "OPD-002, OPD-003");

        risk.setKodePerangkatYangMenangani(null);
        assertHandlingFieldsInBothDetails(risk.getKodeRisiko(), null, null);
    }

    @Test
    void detailEndpointsSerializeHandlingFields() throws Exception {
        RisikoOpd risk = risk();
        risk.setKodePerangkatYangMenangani("OPD-002, OPD-003");
        String kodeRisiko = risk.getKodeRisiko();
        VerifikasiRisiko verification = VerifikasiRisiko.builder().kodeRisiko(kodeRisiko).status("VERIFIED").build();
        when(opdRepository.findByKodeRisiko(kodeRisiko)).thenReturn(Optional.of(risk));
        when(verifikasiRepository.findByKodeRisiko(kodeRisiko)).thenReturn(Optional.of(verification));
        var mvc = MockMvcBuilders.standaloneSetup(new RisikoVerifikasiController(service)).build();

        for (String route : List.of("/pengajuan-risiko/{kodeRisiko}", "/verifikasi-risiko/{kodeRisiko}")) {
            mvc.perform(get(route, kodeRisiko))
                  .andExpect(status().isOk())
                  .andExpect(jsonPath("$.data.risiko.perangkat_yang_menangani").hasJsonPath())
                  .andExpect(jsonPath("$.data.risiko.perangkat_yang_menangani").value(nullValue()))
                  .andExpect(jsonPath("$.data.risiko.kode_perangkat_yang_menangani").value("OPD-002, OPD-003"));
        }
    }

    private void assertHandlingFieldsInBothDetails(String kodeRisiko, String nama, String kodePerangkat) {
        VerifikasiRisiko verification = VerifikasiRisiko.builder().kodeRisiko(kodeRisiko).status("VERIFIED").build();
        when(verifikasiRepository.findByKodeRisiko(kodeRisiko)).thenReturn(Optional.of(verification));

        assertHandlingFields(service.detailPengajuan(kodeRisiko, principal("U-ANISA", "level_3")), nama, kodePerangkat);
        assertHandlingFields(service.detailVerifikasi(kodeRisiko, principal("U-BUDI", "level_2")), nama, kodePerangkat);
    }

    private void assertHandlingFields(Map<String, Object> detail, String nama, String kodePerangkat) {
        Map<?, ?> risk = assertInstanceOf(Map.class, detail.get("risiko"));
        assertTrue(risk.containsKey("perangkat_yang_menangani"));
        assertTrue(risk.containsKey("kode_perangkat_yang_menangani"));
        assertEquals(nama, risk.get("perangkat_yang_menangani"));
        assertEquals(kodePerangkat, risk.get("kode_perangkat_yang_menangani"));
    }

    private RisikoOpd risk() {
        return RisikoOpd.builder().id(102L).kodeRisiko("RSK-OPD-0102").kodeOpd("OPD-001")
              .createdBy("U-ANISA").createdName("Anisa").tahun(2026)
              .pernyataanRisiko("Risiko data tidak lengkap").build();
    }

    private RisikoOpd complete(RisikoOpd risk) {
        risk.setSkalaKemungkinan(3);
        risk.setSkalaDampak(4);
        risk.setRencanaTindakPengendalian("Perbaiki data");
        risk.setKodePerangkatYangMenangani("OPD-001");
        return risk;
    }

    private PemohonRequest pemohon(String id, String nama) {
        PemohonRequest value = new PemohonRequest();
        value.setId(id);
        value.setNama(nama);
        return value;
    }

    private VerifikatorRequest verifikator() {
        VerifikatorRequest value = new VerifikatorRequest();
        value.setId("U-BUDI");
        value.setNama("Budi Santoso");
        return value;
    }

    private cc.kertaskerja.manrisk.security.RiskPrincipal principal(String id, String role) {
        return new cc.kertaskerja.manrisk.security.RiskPrincipal(id, "Budi", null, "OPD-001", Set.of(role));
    }
}
