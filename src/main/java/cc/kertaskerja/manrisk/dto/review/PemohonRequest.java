package cc.kertaskerja.manrisk.dto.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class PemohonRequest {
    @NotBlank
    @Size(max = 128)
    private String id;

    @NotBlank
    @Size(max = 255)
    private String nama;
}
