package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitShiftLogRequest(
    @NotBlank String zone,
    @NotBlank String shiftType,
    @NotBlank String mineralType,
    @NotNull @Positive java.math.BigDecimal volumeExtracted,
    @NotBlank String unit,
    @NotBlank String equipmentCode,
    @NotBlank String equipmentName,
    String notes
) {}
