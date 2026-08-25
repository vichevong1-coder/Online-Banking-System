package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.bill.entity.BillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBillProviderRequest(
        @NotBlank(message = "Provider name is required")
        String name,

        @NotNull(message = "Category is required")
        BillCategory category,

        String accountNumberPattern,
        Boolean active
) {}
