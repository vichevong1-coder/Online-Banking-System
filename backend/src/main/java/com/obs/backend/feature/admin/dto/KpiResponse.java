package com.obs.backend.feature.admin.dto;

import java.math.BigDecimal;

public record KpiResponse(
        long totalCustomers,
        long totalAccounts,
        long failedLogins,
        long todayTransfers,
        BigDecimal todayVolume,
        String displayCurrency) {}
