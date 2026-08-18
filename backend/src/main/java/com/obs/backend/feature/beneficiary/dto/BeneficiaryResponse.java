package com.obs.backend.feature.beneficiary.dto;

import java.time.Instant;
import java.util.UUID;

/** US-029 / US-030: one saved payee, as returned by every beneficiary endpoint. */
public record BeneficiaryResponse(
        UUID id,
        String displayName,
        String bankCode,
        String accountNumber,
        // US-031 quick transfer reads this in Sprint 5; PATCH is what sets it.
        boolean favorite,
        Instant createdAt,
        Instant updatedAt) {
}
