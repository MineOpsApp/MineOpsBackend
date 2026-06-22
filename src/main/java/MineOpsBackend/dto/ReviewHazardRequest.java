package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewHazardRequest(
    @NotBlank String actionTaken
) {}