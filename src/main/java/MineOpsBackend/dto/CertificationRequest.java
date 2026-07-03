package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CertificationRequest(
    Long workerId,               // required on create, ignored on update
    @NotBlank String certificationName,
    @NotBlank String issuingAuthority,
    @NotNull String issueDate,   // YYYY-MM-DD
    @NotNull String expiryDate,  // YYYY-MM-DD
    String notes
) {}
