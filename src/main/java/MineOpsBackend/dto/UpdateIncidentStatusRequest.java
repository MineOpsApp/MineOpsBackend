package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateIncidentStatusRequest(
    @NotBlank @Pattern(regexp = "Open|Under Investigation|Closed") String status
) {}