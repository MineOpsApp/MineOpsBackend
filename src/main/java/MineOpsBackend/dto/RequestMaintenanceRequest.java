package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestMaintenanceRequest(
    @NotBlank String equipmentCode,
    @NotBlank String requestDetails
) {}

