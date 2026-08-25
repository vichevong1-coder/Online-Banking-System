package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.bill.entity.BillCategory;
import java.time.Instant;
import java.util.UUID;

public record BillProviderResponse(
        UUID id,
        String name,
        BillCategory category,
        String accountNumberPattern,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
