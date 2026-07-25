package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BlastDecisionRequest(
    @NotBlank
    @Pattern(regexp = "APPROVED|WAIT|STOP", message = "Decision must be APPROVED, WAIT, or STOP")
    String decision,
    String note
) {}
