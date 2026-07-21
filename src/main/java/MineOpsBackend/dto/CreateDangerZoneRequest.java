package MineOpsBackend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDangerZoneRequest(
    @NotBlank String site,
    @NotBlank String zoneName,
    @NotBlank @Pattern(regexp = "Low|Medium|High", message = "riskLevel must be Low, Medium, or High") String riskLevel,
    @DecimalMin(value = "-90.0",  message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0",   message = "latitude must be between -90 and 90")
    Double latitude,
    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0",  message = "longitude must be between -180 and 180")
    Double longitude,
    Integer radiusMeters
) {}
