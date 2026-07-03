package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemGuestCodeRequest(
    @NotBlank(message = "Code is required") String code,
    @NotBlank(message = "Full name is required") String fullName,
    @NotBlank(message = "Phone number is required") String phone
) {}
