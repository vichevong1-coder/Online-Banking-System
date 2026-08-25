package com.obs.backend.feature.bill.dto;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.bill.entity.BillCategory;
import com.obs.backend.feature.bill.entity.PaymentFrequency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringBillPaymentResponse(
        UUID id,
        UUID providerId,
        String providerName,
        BillCategory providerCategory,
        UUID accountId,
        String accountNumber,
        String billAccountNumber,
        BigDecimal amount,
        Currency currency,
        PaymentFrequency frequency,
        LocalDate nextPaymentDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
