package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIllegalMineReportRequest(
    @NotBlank String locationDescription,
    String details,
    String photoData,
    Double latitude,
    Double longitude
) {}
