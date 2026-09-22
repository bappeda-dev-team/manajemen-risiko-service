package cc.kertaskerja.manrisk.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = false)
public record GenerateAiReqDTO(
        @NotBlank String requestId,
        @NotBlank String type,
        @NotNull @Valid Scope scope,
        @NotBlank String contextVersion,
        @NotNull Map<String, String> input
) {
    @JsonIgnoreProperties(ignoreUnknown = false)
    public record Scope(
            @NotBlank String kodeOpd,
            @NotBlank String kodeSasaran,
            @NotNull Integer tahun,
            String kodeIndikator
    ) {}
}
