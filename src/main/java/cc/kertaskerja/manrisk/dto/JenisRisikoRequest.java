package cc.kertaskerja.manrisk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JenisRisikoRequest(
      @NotBlank(message = "Nama jenis risiko wajib diisi")
      @Size(max = 120, message = "Nama jenis risiko maksimal 120 karakter")
      String nama
) { }
