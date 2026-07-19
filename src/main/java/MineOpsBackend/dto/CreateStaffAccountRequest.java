package MineOpsBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateStaffAccountRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank @Pattern(regexp = "supervisor|safetyOfficer", message = "role must be supervisor or safetyOfficer") String role,
    @NotBlank String assignedSite
) {}
