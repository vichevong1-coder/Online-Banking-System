package com.obs.backend.feature.card.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record UpdateCardRequest(
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
        String pin,

        @DecimalMin(value = "0.01", message = "Daily limit must be greater than zero")
        BigDecimal dailyLimit,

        @DecimalMin(value = "0.01", message = "Per-transaction limit must be greater than zero")
        BigDecimal perTransactionLimit
) {}
