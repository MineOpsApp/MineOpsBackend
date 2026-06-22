package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEquipmentStatusRequest(
    @NotBlank String equipmentId,
    String status
) {}
