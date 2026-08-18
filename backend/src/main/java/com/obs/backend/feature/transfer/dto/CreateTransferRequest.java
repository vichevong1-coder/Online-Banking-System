package com.obs.backend.feature.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * US-025: a transfer between two of the caller's own accounts.
 *
 * <p>Bean Validation covers only what is decidable from the payload alone.
 * Ownership, currency match, limits and available funds all need the accounts
 * loaded, so they are enforced in the service and reported as coded 4xx errors.
 */
public record CreateTransferRequest(
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        // Four decimal places matches transfers.amount NUMERIC(19,4).
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 255) String description) {
}
