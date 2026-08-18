package com.obs.backend.feature.admin.dto;

import com.obs.backend.security.AccountStatus;
import java.time.Instant;
import java.util.UUID;

// Deliberately omits passwordHash and the KYC fields (nidNumber, nidExpiryDate, dateOfBirth).
// The customer table doesn't display them, and a list endpoint is the worst place to leak them.
// Note there is no email: customers are identified by phone and users.email is NULL for them.
public record CustomerSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        AccountStatus status,
        boolean phoneVerified,
        Instant createdAt) {
}
