package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PreviewPayCycleRequest(
    @NotBlank(message = "Pay date is required (YYYY-MM-DD)") String payDate,
    @NotBlank(message = "Mineral type is required") String mineralType,
    @NotBlank(message = "Unit is required") String unit,
    @NotNull(message = "Price per unit is required")
    @Positive(message = "Price per unit must be greater than zero")
    BigDecimal pricePerUnit
) {}
