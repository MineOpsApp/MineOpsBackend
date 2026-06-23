package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record StartDrillRequest(
    @NotBlank String zone,
    @NotBlank String equipmentCode,
    @NotBlank String drillType
) {}
