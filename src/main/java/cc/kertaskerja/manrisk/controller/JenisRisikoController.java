package cc.kertaskerja.manrisk.controller;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import cc.kertaskerja.manrisk.dto.JenisRisikoRequest;
import cc.kertaskerja.manrisk.dto.JenisRisikoResponse;
import cc.kertaskerja.manrisk.service.risiko.JenisRisikoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jenis-risiko")
@Validated
@RequiredArgsConstructor
@Tag(name = "Master Jenis Risiko")
public class JenisRisikoController {
    private final JenisRisikoService service;

    @GetMapping
    @Operation(summary = "Ambil semua jenis risiko")
    public ResponseEntity<ApiResponse<List<JenisRisikoResponse>>> findAll() {
        List<JenisRisikoResponse> result = service.findAll();
        return ResponseEntity.ok(ApiResponse.success(result, "Retrieved " + result.size() + " jenis risiko successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ambil jenis risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<JenisRisikoResponse>> findById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.findById(id), "Retrieved 1 jenis risiko successfully"));
    }

    @PostMapping
    @Operation(summary = "Tambah jenis risiko")
    public ResponseEntity<ApiResponse<JenisRisikoResponse>> create(@Valid @RequestBody JenisRisikoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah jenis risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<JenisRisikoResponse>> update(
          @PathVariable @Min(1) Long id, @Valid @RequestBody JenisRisikoRequest request) {
        return ResponseEntity.ok(ApiResponse.updated(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus jenis risiko berdasarkan ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @Min(1) Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
