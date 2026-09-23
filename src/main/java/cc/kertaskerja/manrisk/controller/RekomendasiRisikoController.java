package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiReqDTO;
import cc.kertaskerja.manrisk.dto.ai.GenerateAiResDTO;
import cc.kertaskerja.manrisk.security.RisikoAiInternalAuthFilter;
import cc.kertaskerja.manrisk.service.ai.RekomendasiRisikoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risiko")
@RequiredArgsConstructor
@Tag(name = "Manajemen Risiko")
public class RekomendasiRisikoController {
    private final RekomendasiRisikoService rekomendasiRisikoService;

    @PostMapping("/generate-ai")
    @Operation(summary = "Buat usulan AI risiko OPD", description = "Hanya untuk trusted server frontend yang memakai token internal.")
    public ResponseEntity<ApiResponse<GenerateAiResDTO>> generateAi(
            @Valid @RequestBody GenerateAiReqDTO request,
            HttpServletRequest servletRequest) {
        String caller = (String) servletRequest.getAttribute(RisikoAiInternalAuthFilter.CALLER_ATTRIBUTE);
        GenerateAiResDTO result = rekomendasiRisikoService.generate(caller, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Rekomendasi berhasil dibuat"));
    }
}
