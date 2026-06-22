package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LogEquipmentShiftRequest(
    @NotBlank String equipmentCode,
    @NotBlank String equipmentName,
    @Pattern(regexp = "Operational|Idle|Maintenance|Flagged", message = "Status must be Operational, Idle, Maintenance, or Flagged")
    @NotBlank String status,
    @Pattern(regexp = "ShiftStart|ShiftEnd|MidShiftCheck", message = "Check type must be ShiftStart, ShiftEnd, or MidShiftCheck")
    @NotBlank String checkType,
    String notes
) {}