package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportFaultRequest(
    @NotBlank String equipmentCode,
    @NotBlank String description
) {}
