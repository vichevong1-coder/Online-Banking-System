package com.obs.backend.feature.admin.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import com.obs.backend.security.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// The detail drawer shows the KYC data the customer submitted at registration (US-007), so unlike
// the summary this carries nidNumber / nidExpiryDate / dateOfBirth / gender. passwordHash is never
// exposed at any level.
public record CustomerDetailResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        AccountStatus status,
        boolean phoneVerified,
        String nidNumber,
        LocalDate nidExpiryDate,
        LocalDate dateOfBirth,
        Gender gender,
        Instant createdAt) {
}
