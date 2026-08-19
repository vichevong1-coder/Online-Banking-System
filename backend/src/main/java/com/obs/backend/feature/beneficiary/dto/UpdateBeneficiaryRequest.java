package com.obs.backend.feature.beneficiary.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-030 edit. A partial update: every field is optional and a null one means
 * "leave it alone", so a client renaming a payee does not have to resend the
 * destination it never touched.
 *
 * <p>That makes an explicit JSON null indistinguishable from an absent field.
 * It is the right trade here because no field on a beneficiary is nullable —
 * there is nothing a client could legitimately be asking to clear.
 *
 * <p>The constraints are the create ones minus @NotBlank, which would fire on
 * every omitted field. {@code @Size(min = 1)} still rejects an explicit empty
 * name, and {@code @Pattern} passes silently on null.
 */
public record UpdateBeneficiaryRequest(
        @Size(min = 1, max = 100) String displayName,
        @Pattern(regexp = "^[A-Z0-9]{4,11}$") String bankCode,
        @Pattern(regexp = "^[A-Za-z0-9]{6,34}$") String accountNumber,
        // US-031's flag. Boxed rather than primitive so "not sent" stays
        // distinguishable from "set to false".
        Boolean favorite) {
}
