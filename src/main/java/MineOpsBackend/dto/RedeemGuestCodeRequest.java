package MineOpsBackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RedeemGuestCodeRequest(
    @NotBlank(message = "Code is required") String code,
    @NotBlank(message = "Full name is required") String fullName,
    @NotBlank(message = "Phone number is required") String phone,
    @NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email
) {}
