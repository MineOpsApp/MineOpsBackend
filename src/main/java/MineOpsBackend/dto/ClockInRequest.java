package MineOpsBackend.dto;

public record ClockInRequest(String zone, String notes, String clientRequestId) {}