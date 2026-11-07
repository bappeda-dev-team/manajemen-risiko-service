package cc.kertaskerja.manrisk.dto.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class ReviewDecisionRequest {
    @NotBlank
    private String status;

    @JsonProperty("catatan")
    @Size(max = 2000)
    private String catatan;

    private VerifikatorRequest verifikator;
}
