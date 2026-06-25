package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScheduleBlastRequest(
    @NotBlank String zone,
    @NotNull String blastTime, // ISO datetime string
    String notes
) {}