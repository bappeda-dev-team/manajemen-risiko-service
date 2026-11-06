package cc.kertaskerja.manrisk.dto.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class SubmitVerificationRequest {
    @JsonProperty("kode_risiko")
    @NotBlank
    @Size(max = 255)
    private String kodeRisiko;

    @NotNull
    @Valid
    private PemohonRequest pemohon;
}
