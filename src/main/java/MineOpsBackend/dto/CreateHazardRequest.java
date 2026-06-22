package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateHazardRequest(
    @NotBlank String hazardType,
    @NotBlank String site,
    @NotBlank String location,
    @NotBlank String description
) {}