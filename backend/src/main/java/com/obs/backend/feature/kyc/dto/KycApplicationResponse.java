package com.obs.backend.feature.kyc.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import com.obs.backend.security.AccountStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record KycApplicationResponse(
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        String nidNumber,
        LocalDate nidExpiryDate,
        LocalDate dateOfBirth,
        Gender gender,
        AccountStatus status,
        String rejectionReason,
        Instant submittedAt,
        List<KycDocumentInfo> documents) {
}
