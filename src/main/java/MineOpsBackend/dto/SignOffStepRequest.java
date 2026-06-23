package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignOffStepRequest(
    @NotBlank
    @Pattern(regexp = "setup|drilling|blasting|cleanup", message = "Step must be setup, drilling, blasting, or cleanup")
    String step,
    String notes
) {}
