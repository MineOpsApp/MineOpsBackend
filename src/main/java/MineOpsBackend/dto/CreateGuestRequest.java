package MineOpsBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateGuestRequest(
    @NotBlank String fullName,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String guestSubRole,
    String assignedSite,
    Integer sessionHours,
    String createdByEmail,
    String createdByName
) {}