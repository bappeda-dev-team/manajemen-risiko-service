package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoPemda.RisikoPemdaResDTO;
import cc.kertaskerja.manrisk.service.risiko.RisikoPemdaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/risiko-pemda")
@RequiredArgsConstructor
@Validated
@Tag(name = "Manajemen Risiko Pemda")
public class RisikoPemdaController {
    private final RisikoPemdaService risikoPemdaService;

    @GetMapping
    @Operation(summary = "Ambil semua data risiko Pemda")
    public ResponseEntity<ApiResponse<List<RisikoPemdaResDTO>>> getAllRisiko() {
        List<RisikoPemdaResDTO> result = risikoPemdaService.getAllRisiko();
        return ResponseEntity.ok(ApiResponse.success(result,
              "Retrieved " + result.size() + " data risiko Pemda successfully"));
    }

    @GetMapping("/sasaran/{kodeSasaranPemda}")
    @Operation(summary = "Ambil risiko Pemda berdasarkan kode sasaran dan tab")
    public ResponseEntity<ApiResponse<RisikoPemdaResDTO>> getBySasaran(
          @PathVariable @NotBlank @Size(max = 128) String kodeSasaranPemda,
          @RequestParam @NotBlank String type) {
        return ResponseEntity.ok(ApiResponse.success(
              risikoPemdaService.getRisikoByKodeSasaranPemda(kodeSasaranPemda, type), ""));
    }

    @GetMapping("/{kodeRisiko}")
    @Operation(summary = "Ambil detail risiko Pemda berdasarkan kode risiko")
    public ResponseEntity<ApiResponse<RisikoPemdaResDTO>> getByKodeRisiko(
          @PathVariable @NotBlank @Size(max = 128) String kodeRisiko) {
        return ResponseEntity.ok(ApiResponse.success(risikoPemdaService.getRisikoByKodeRisiko(kodeRisiko),
              "Retrieved 1 data successfully"));
    }

    @PostMapping
    @Operation(summary = "Simpan risiko Pemda baru")
    public ResponseEntity<ApiResponse<RisikoPemdaResDTO>> create(
          @Valid @RequestBody RisikoPemdaReqDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponse.created(risikoPemdaService.createRisiko(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah risiko Pemda berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoPemdaResDTO>> update(
          @PathVariable @Min(1) Long id,
          @Valid @RequestBody RisikoPemdaReqDTO request) {
        return ResponseEntity.ok(ApiResponse.updated(risikoPemdaService.updateRisiko(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus risiko Pemda berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Min(1) Long id) {
        risikoPemdaService.deleteRisiko(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
