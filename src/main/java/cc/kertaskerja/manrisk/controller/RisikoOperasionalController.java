package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOperasional.RisikoOperasionalResDTO;
import cc.kertaskerja.manrisk.service.risiko.RisikoOperasionalService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risiko-operasional")
@RequiredArgsConstructor
@Validated
@Tag(name = "Manajemen Risiko Operasional")
public class RisikoOperasionalController {
    private final RisikoOperasionalService risikoOperasionalService;

    @GetMapping
    @Operation(summary = "Ambil semua data risiko Operasional")
    public ResponseEntity<ApiResponse<List<RisikoOperasionalResDTO>>> getAllRisiko() {
        List<RisikoOperasionalResDTO> result = risikoOperasionalService.getAllRisiko();
        return ResponseEntity.ok(ApiResponse.success(result,
              "Retrieved " + result.size() + " data risiko Operasional successfully"));
    }

    @GetMapping("/rekin/{kodeRekin}")
    @Operation(summary = "Ambil risiko Operasional berdasarkan rencana kinerja dan tab")
    public ResponseEntity<ApiResponse<RisikoOperasionalResDTO>> getByRekin(
          @PathVariable @NotBlank @Size(max = 128) String kodeRekin,
          @RequestParam @NotBlank String type) {
        return ResponseEntity.ok(ApiResponse.success(
              risikoOperasionalService.getRisikoByKodeRekin(kodeRekin, type), ""));
    }

    @GetMapping("/{kodeRisiko}")
    @Operation(summary = "Ambil detail risiko Operasional berdasarkan kode risiko")
    public ResponseEntity<ApiResponse<RisikoOperasionalResDTO>> getByKodeRisiko(
          @PathVariable @NotBlank @Size(max = 128) String kodeRisiko) {
        return ResponseEntity.ok(ApiResponse.success(
              risikoOperasionalService.getRisikoByKodeRisiko(kodeRisiko), "Retrieved 1 data successfully"));
    }

    @PostMapping
    @Operation(summary = "Simpan risiko Operasional baru")
    public ResponseEntity<ApiResponse<RisikoOperasionalResDTO>> create(
          @Valid @RequestBody RisikoOperasionalReqDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponse.created(risikoOperasionalService.createRisiko(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah risiko Operasional berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoOperasionalResDTO>> update(
          @PathVariable @Min(1) Long id,
          @Valid @RequestBody RisikoOperasionalReqDTO request) {
        return ResponseEntity.ok(ApiResponse.updated(risikoOperasionalService.updateRisiko(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus risiko Operasional berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Min(1) Long id) {
        risikoOperasionalService.deleteRisiko(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
