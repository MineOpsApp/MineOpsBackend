package MineOpsBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RenewGuestSessionRequest(
    @NotBlank @Email String email,
    Integer hours
) {}

