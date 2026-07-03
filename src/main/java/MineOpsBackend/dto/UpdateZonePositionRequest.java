package MineOpsBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateZonePositionRequest(
    @NotNull @Size(min = 3, message = "A polygon requires at least 3 points") List<MapPoint> points
) {
    public record MapPoint(@NotNull Double x, @NotNull Double y) {}
}
