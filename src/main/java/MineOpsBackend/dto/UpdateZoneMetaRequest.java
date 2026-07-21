package MineOpsBackend.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateZoneMetaRequest(
    String zoneName,
    @Pattern(regexp = "Low|Medium|High", message = "riskLevel must be Low, Medium, or High")
    String riskLevel
) {}
