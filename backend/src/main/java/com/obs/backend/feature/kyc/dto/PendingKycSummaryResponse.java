package com.obs.backend.feature.kyc.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingKycSummaryResponse(
        UUID userId, String firstName, String lastName, String phone, Instant registeredAt, long documentCount) {
}
