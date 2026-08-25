package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.bill.entity.BillCategory;

public record UpdateBillProviderRequest(
        String name,
        BillCategory category,
        String accountNumberPattern,
        Boolean active
) {}
