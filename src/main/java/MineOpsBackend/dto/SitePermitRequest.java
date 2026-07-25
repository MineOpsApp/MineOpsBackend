package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SitePermitRequest(
    @NotBlank String permitName,
    String permitNumber,
    @NotBlank String issuingAuthority,
    @NotNull String issueDate,   // YYYY-MM-DD
    @NotNull String expiryDate,  // YYYY-MM-DD
    String notes,
    String documentData          // base64-encoded image, optional
) {}
