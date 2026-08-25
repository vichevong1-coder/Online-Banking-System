package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.bill.entity.BillCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillPaymentResponse(
        UUID id,
        UUID providerId,
        String providerName,
        BillCategory providerCategory,
        UUID accountId,
        String accountNumber,
        String billAccountNumber,
        BigDecimal amount,
        Currency currency,
        UUID transferId,
        String reference,
        String status,
        Instant createdAt
) {}
