package MineOpsBackend.security;

public record AuthenticatedUser(
    Long id,
    String fullName,
    String email,
    String role,
    String assignedSite,
    String guestSubRole
) {
    public String authority() {
        return authorityFor(role);
    }

    public static String authorityFor(String role) {
        return switch (role) {
            case "worker" -> "ROLE_WORKER";
            case "supervisor" -> "ROLE_SUPERVISOR";
            case "safetyOfficer" -> "ROLE_SAFETY_OFFICER";
            case "guest" -> "ROLE_GUEST";
            default -> "ROLE_UNKNOWN";
        };
    }
}