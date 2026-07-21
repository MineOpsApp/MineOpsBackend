package MineOpsBackend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateZoneGpsRequest(
    @NotNull Double latitude,
    @NotNull Double longitude,
    Integer radiusMeters
) {}
