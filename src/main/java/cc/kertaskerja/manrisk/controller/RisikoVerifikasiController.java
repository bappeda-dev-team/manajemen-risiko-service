package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.review.ReviewDecisionRequest;
import cc.kertaskerja.manrisk.dto.review.SubmitVerificationRequest;
import cc.kertaskerja.manrisk.security.RiskPrincipal;
import cc.kertaskerja.manrisk.security.RiskPrincipalContext;
import cc.kertaskerja.manrisk.service.risiko.RisikoVerifikasiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Pengajuan dan Verifikasi Risiko")
public class RisikoVerifikasiController {
    private final RisikoVerifikasiService service;

    @GetMapping("/pengajuan-risiko")
    @Operation(summary = "Ambil seluruh pengajuan risiko milik pemohon")
    public ResponseEntity<ApiResponse<Map<String, Object>>> daftarPengajuan() {
        RiskPrincipal actor = RiskPrincipalContext.current();
        return ResponseEntity.ok(ApiResponse.success(actor == null ? service.daftarPengajuan() : service.daftarPengajuan(actor), "Daftar pengajuan risiko"));
    }

    @GetMapping("/pengajuan-risiko/{kodeRisiko}")
    @Operation(summary = "Ambil detail pengajuan risiko milik pemohon")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailPengajuan(
          @PathVariable @NotBlank @Size(max = 255) String kodeRisiko) {
        RiskPrincipal actor = RiskPrincipalContext.current();
        return ResponseEntity.ok(ApiResponse.success(actor == null ? service.detailPengajuan(kodeRisiko) : service.detailPengajuan(kodeRisiko, actor), "Detail pengajuan risiko"));
    }

    @GetMapping("/verifikasi-risiko")
    @Operation(summary = "Ambil pengajuan risiko untuk verifikasi dalam OPD")
    public ResponseEntity<ApiResponse<Map<String, Object>>> daftarVerifikasi() {
        RiskPrincipal actor = RiskPrincipalContext.current();
        return ResponseEntity.ok(ApiResponse.success(actor == null ? service.daftarVerifikasi() : service.daftarVerifikasi(actor), "Daftar verifikasi risiko"));
    }

    @GetMapping("/verifikasi-risiko/{kodeRisiko}")
    @Operation(summary = "Ambil detail risiko untuk verifikasi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailVerifikasi(
          @PathVariable @NotBlank @Size(max = 255) String kodeRisiko) {
        RiskPrincipal actor = RiskPrincipalContext.current();
        return ResponseEntity.ok(ApiResponse.success(actor == null ? service.detailVerifikasi(kodeRisiko) : service.detailVerifikasi(kodeRisiko, actor), "Detail verifikasi risiko"));
    }

    @PostMapping("/verifikasi-risiko")
    @Operation(summary = "Ajukan risiko draf untuk diverifikasi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submit(
          @Valid @RequestBody SubmitVerificationRequest body) {
        RiskPrincipal actor = RiskPrincipalContext.current();
        return ResponseEntity.status(HttpStatus.CREATED)
              .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Risiko berhasil diajukan", actor == null ? service.submit(body) : service.submit(body, actor)));
    }

    @PatchMapping("/verifikasi-risiko/{kodeRisiko}/keputusan")
    @Operation(summary = "Setujui atau tolak pengajuan risiko")
    public ResponseEntity<ApiResponse<Map<String, Object>>> keputusan(
          @PathVariable @NotBlank @Size(max = 255) String kodeRisiko,
          @Valid @RequestBody ReviewDecisionRequest body) {
        RiskPrincipal actor = RiskPrincipalContext.current();
        Map<String, Object> result = actor == null ? service.decide(kodeRisiko, body) : service.decide(kodeRisiko, body, actor);
        String status = (String) result.get("status");
        String message = "VERIFIED".equals(status) ? "Risiko berhasil diverifikasi" : "Risiko ditolak";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }
}
