package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateIncidentRequest(
    @NotBlank String zone,
    @NotBlank @Pattern(regexp = "Injury|Near Miss|Equipment Damage|Environmental") String category,
    @NotBlank @Pattern(regexp = "Minor|Serious|Critical") String severity,
    @NotBlank String description,
    String involvedPersons,
    Boolean firstAidGiven,
    Boolean hospitalRequired,
    String immediateAction,
    Double latitude,
    Double longitude,
    String photoData,
    String incidentAt
) {}