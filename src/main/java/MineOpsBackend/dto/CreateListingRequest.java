package MineOpsBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateListingRequest(
    @NotBlank String mineralType,
    @NotNull @Positive BigDecimal quantity,
    @NotBlank String unit,
    String grade,
    @NotNull @Positive BigDecimal askingPrice,
    String location,
    String availableFrom,
    @Positive BigDecimal minOrderQuantity,
    String photoData
) {}
