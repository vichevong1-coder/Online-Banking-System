package com.obs.backend.feature.user.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String nidNumber,
        LocalDate nidExpiryDate,
        LocalDate dateOfBirth,
        Gender gender,
        String email,
        Role role,
        AccountStatus status,
        Instant createdAt
) {}
