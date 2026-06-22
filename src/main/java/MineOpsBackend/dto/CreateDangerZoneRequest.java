package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDangerZoneRequest(
    @NotBlank String site,
    @NotBlank String zoneName,
    @NotBlank @Pattern(regexp = "Low|Medium|High", message = "riskLevel must be Low, Medium, or High") String riskLevel
) {}

