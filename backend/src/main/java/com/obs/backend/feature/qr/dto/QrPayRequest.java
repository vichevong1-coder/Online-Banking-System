package com.obs.backend.feature.qr.dto;

import com.obs.backend.feature.account.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * US-033/US-034: pay a scanned payload.
 *
 * <p>{@code amount} and {@code currency} are optional here rather than required,
 * because whether they are needed depends on the payload: a merchant code that
 * fixes its own price may only be agreed with, and one that does not must be
 * given an amount. Bean Validation cannot see the payload, so that pairing is
 * enforced in the service — as {@code QR_AMOUNT_MISMATCH} and
 * {@code VALIDATION_FAILED} respectively.
 */
public record QrPayRequest(
        @NotBlank @Size(max = 255) String payload,
        @NotNull UUID fromAccountId,
        // Four decimal places matches transfers.amount NUMERIC(19,4).
        @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        Currency currency,
        @Size(max = 255) String description) {
}
