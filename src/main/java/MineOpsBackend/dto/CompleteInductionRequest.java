package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteInductionRequest(
    @NotBlank String visitorType,
    @NotBlank String site
) {}