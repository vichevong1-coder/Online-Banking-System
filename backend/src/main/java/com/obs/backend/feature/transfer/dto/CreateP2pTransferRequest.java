package com.obs.backend.feature.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * US-026: a transfer to another customer's account at this bank.
 *
 * <p>Distinct from {@link CreateTransferRequest} in one way that matters: the
 * destination is named by <em>account number</em>, not by id, and it is not the
 * caller's. A customer knows the number someone gave them; they cannot know that
 * account's UUID, and accepting one here would let a caller probe ids for
 * existence. It is also why this is a separate endpoint rather than a nullable
 * field on US-025 — "both accounts are yours" is the invariant that makes that
 * one simple to reason about.
 *
 * <p>Currency is not a field: it is the source account's, and a destination that
 * disagrees is rejected rather than converted, exactly as in US-025.
 *
 * <p>The payee's name is not a field either. Nothing here is verified against
 * the name held on the destination account, so accepting one would only invite
 * the client to display an unchecked claim as if the bank had confirmed it.
 */
public record CreateP2pTransferRequest(
        @NotNull UUID fromAccountId,
        // Same shape as the account numbers this system issues.
        @NotBlank @Pattern(regexp = "^[0-9]{6,20}$") String toAccountNumber,
        // Four decimal places matches transfers.amount NUMERIC(19,4).
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 255) String description) {
}
