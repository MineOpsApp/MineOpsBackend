package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateGuestCodeRequest(
    @NotBlank(message = "Guest sub-role is required") String guestSubRole,
    @NotNull @Positive Integer sessionHours,
    @NotNull @Positive Integer maxRedemptions,
    @NotBlank(message = "Expiry date/time is required") String expiresAt
) {}
