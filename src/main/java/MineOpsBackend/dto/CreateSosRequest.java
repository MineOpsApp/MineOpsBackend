package MineOpsBackend.dto;

public record CreateSosRequest(
    String site,
    String message,
    String clientRequestId,
    Double latitude,
    Double longitude
) {}

