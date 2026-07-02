package MineOpsBackend.dto;

public record SaveEmergencyContactRequest(
    String contactType,
    String name,
    String relationship,
    String phone
) {}
