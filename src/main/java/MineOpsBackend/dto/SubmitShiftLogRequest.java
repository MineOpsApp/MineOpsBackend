package MineOpsBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitShiftLogRequest(
    @NotBlank(message = "Zone is required") String zone,
    @NotBlank(message = "Shift type is required") String shiftType,
    @NotBlank(message = "Mineral type is required") String mineralType,
    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be greater than zero")
    @Digits(integer = 10, fraction = 3, message = "Volume allows up to 3 decimal places")
    java.math.BigDecimal volumeExtracted,
    @NotBlank(message = "Unit is required") String unit,
    @NotBlank(message = "Equipment code is required") String equipmentCode,
    @NotBlank(message = "Equipment name is required") String equipmentName,
    String notes,
    String shiftDate,
    String clientRequestId
) {}
