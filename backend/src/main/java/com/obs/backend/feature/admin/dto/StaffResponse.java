package com.obs.backend.feature.admin.dto;

import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        Instant createdAt) {}
