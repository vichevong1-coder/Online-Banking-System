package com.obs.backend.feature.card.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CreateCardRequest(
        @NotNull(message = "Account ID is required")
        UUID accountId,

        String cardHolderName,

        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
        String pin
) {}
