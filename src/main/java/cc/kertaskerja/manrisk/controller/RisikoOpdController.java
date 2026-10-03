package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdResDTO;
import cc.kertaskerja.manrisk.service.risiko.RisikoOpdService;
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
@RequestMapping("/risiko-opd")
@RequiredArgsConstructor
@Validated
@Tag(name = "Manajemen Risiko OPD")
public class RisikoOpdController {
    private final RisikoOpdService risikoOpdService;

    @GetMapping
    @Operation(summary = "Ambil semua data risiko OPD")
    public ResponseEntity<ApiResponse<List<RisikoOpdResDTO>>> getAllRisiko() {
        List<RisikoOpdResDTO> result = risikoOpdService.getAllRisiko();
        return ResponseEntity.ok(ApiResponse.success(result,
              "Retrieved " + result.size() + " data risiko OPD successfully"));
    }

    @GetMapping("/sasaran/{kodeSasaranOpd}")
    @Operation(summary = "Ambil risiko OPD berdasarkan kode sasaran dan tab")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> getBySasaran(
          @PathVariable @NotBlank @Size(max = 128) String kodeSasaranOpd,
          @RequestParam @NotBlank String type) {
        return ResponseEntity.ok(ApiResponse.success(
              risikoOpdService.getRisikoByKodeSasaranOpd(kodeSasaranOpd, type), ""));
    }

    @GetMapping("/{kodeRisiko}")
    @Operation(summary = "Ambil detail risiko OPD berdasarkan kode risiko")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> getByKodeRisiko(
          @PathVariable @NotBlank @Size(max = 128) String kodeRisiko) {
        return ResponseEntity.ok(ApiResponse.success(
              risikoOpdService.getRisikoByKodeRisiko(kodeRisiko), "Retrieved 1 data successfully"));
    }

    @PostMapping
    @Operation(summary = "Simpan risiko OPD baru")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> create(
          @Valid @RequestBody RisikoOpdReqDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponse.created(risikoOpdService.createRisiko(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah risiko OPD berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> update(
          @PathVariable @Min(1) Long id,
          @Valid @RequestBody RisikoOpdReqDTO request) {
        return ResponseEntity.ok(ApiResponse.updated(risikoOpdService.updateRisiko(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus risiko OPD berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Min(1) Long id) {
        risikoOpdService.deleteRisiko(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
