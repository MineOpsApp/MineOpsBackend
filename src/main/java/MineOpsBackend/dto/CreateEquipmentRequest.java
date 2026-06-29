package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEquipmentRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String type,
    String notes
) {}