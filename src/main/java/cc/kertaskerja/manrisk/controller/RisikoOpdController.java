package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdReqDTO;
import cc.kertaskerja.manrisk.dto.RisikoOpd.RisikoOpdResDTO;
import cc.kertaskerja.manrisk.service.RisikoOpdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/risiko")
@RequiredArgsConstructor
@Tag(name = "Manajemen Risiko")
public class RisikoOpdController {

    private final RisikoOpdService risikoOpdService;

    @GetMapping
    @Operation(summary = "Ambil semua data risiko")
    public ResponseEntity<ApiResponse<List<RisikoOpdResDTO>>> getAllRisiko() {
        List<RisikoOpdResDTO> result = risikoOpdService.getAllRisiko();

        return ResponseEntity.ok(ApiResponse.success(result, "Retrieved " + result.size() + " data risiko successfully"));
    }

    @GetMapping("/sasaran/{kodeSasaranOpd}")
    @Operation(summary = "Ambil data risiko berdasarkan kode sasaran")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> getRisikoByKodeSasaranOpd(
          @PathVariable String kodeSasaranOpd,
          @RequestParam(required = false) String type) {

        RisikoOpdResDTO result = risikoOpdService.getRisikoByKodeSasaranOpd(kodeSasaranOpd, type);

        return ResponseEntity.ok(ApiResponse.success(result, ""));
    }

    @GetMapping("/{kodeRisiko}")
    @Operation(summary = "Ambil data risiko berdasarkan Kode Risiko")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> getRisikoByKodeRisiko(@PathVariable String kodeRisiko) {
        RisikoOpdResDTO result = risikoOpdService.getRisikoByKodeRisiko(kodeRisiko);

        return ResponseEntity.ok(ApiResponse.success(result, "Retrieved 1 data successfully"));
    }

    @PostMapping
    @Operation(summary = "Simpan data risiko baru")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> createRisiko(@Valid @RequestBody RisikoOpdReqDTO reqDTO) {
        RisikoOpdResDTO created = risikoOpdService.createRisiko(reqDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah data risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoOpdResDTO>> updateRisiko(
          @PathVariable Long id,
          @Valid @RequestBody RisikoOpdReqDTO reqDTO) {
        RisikoOpdResDTO updated = risikoOpdService.updateRisiko(id, reqDTO);

        return ResponseEntity.ok(ApiResponse.updated(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus data risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> deleteRisiko(@PathVariable Long id) {
        risikoOpdService.deleteRisiko(id);

        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
