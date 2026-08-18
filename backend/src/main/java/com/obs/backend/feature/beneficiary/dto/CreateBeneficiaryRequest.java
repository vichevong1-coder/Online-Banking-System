package com.obs.backend.feature.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-029: saves a payee the customer expects to transfer to again.
 *
 * <p>The destination shapes are copied from
 * {@code CreateExternalTransferRequest} on purpose — a beneficiary that the
 * interbank endpoint would reject is worth nothing, so the two validations have
 * to agree. Neither is checked against a real bank directory; there is no
 * interbank integration to check against.
 *
 * <p>Whether the destination is already saved needs the caller's existing rows
 * and so is reported as a coded 4xx, not by Bean Validation.
 */
public record CreateBeneficiaryRequest(
        // What the customer calls this payee, e.g. "Landlord" — not verified
        // against the name held at the receiving bank, which we cannot see.
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{4,11}$") String bankCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{6,34}$") String accountNumber) {
}
