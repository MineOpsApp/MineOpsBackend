package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateHazardRequest(
    @NotBlank String hazardType,
    @NotBlank String site,
    @NotBlank String location,
    @NotBlank String description,
    @Pattern(regexp = "Low|Medium|High|Critical", message = "Severity must be Low, Medium, High, or Critical")
    String severity,
    Double latitude,
    Double longitude,
    String photoData
) {}