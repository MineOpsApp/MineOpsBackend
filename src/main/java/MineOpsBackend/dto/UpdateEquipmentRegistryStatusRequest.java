package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateEquipmentRegistryStatusRequest(
    @NotBlank @Pattern(regexp = "Operational|Idle|Maintenance|Flagged") String status,
    String notes
) {}