package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoReqDTO;
import cc.kertaskerja.manrisk.dto.Risiko.RisikoResDTO;
import cc.kertaskerja.manrisk.service.risiko.RisikoService;
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
public class RisikoController {

    private final RisikoService risikoService;

    @GetMapping
    @Operation(summary = "Ambil semua data risiko")
    public ResponseEntity<ApiResponse<List<RisikoResDTO>>> getAllRisiko() {
        List<RisikoResDTO> result = risikoService.getAllRisiko();

        return ResponseEntity.ok(ApiResponse.success(result, "Retrieved " + result.size() + " data risiko successfully"));
    }

    @GetMapping("/sasaran/{kodeSasaranOpd}")
    @Operation(summary = "Ambil data risiko berdasarkan kode sasaran")
    public ResponseEntity<ApiResponse<List<RisikoResDTO>>> getRisikoByKodeSasaranOpd(
          @PathVariable String kodeSasaranOpd,
          @RequestParam(required = false) String type) {
        List<RisikoResDTO> result = risikoService.getRisikoByKodeSasaranOpd(kodeSasaranOpd, type);

        return ResponseEntity.ok(ApiResponse.success(result, ""));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil data risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoResDTO>> getRisikoById(@PathVariable Long id) {
        RisikoResDTO result = risikoService.getRisikoById(id);

        return ResponseEntity.ok(ApiResponse.success(result, "Retrieved 1 data successfully"));
    }

    @PostMapping
    @Operation(summary = "Simpan data risiko baru")
    public ResponseEntity<ApiResponse<RisikoResDTO>> createRisiko(@Valid @RequestBody RisikoReqDTO reqDTO) {
        RisikoResDTO created = risikoService.createRisiko(reqDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah data risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<RisikoResDTO>> updateRisiko(
          @PathVariable Long id,
          @Valid @RequestBody RisikoReqDTO reqDTO) {
        RisikoResDTO updated = risikoService.updateRisiko(id, reqDTO);

        return ResponseEntity.ok(ApiResponse.updated(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus data risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> deleteRisiko(@PathVariable Long id) {
        risikoService.deleteRisiko(id);

        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
