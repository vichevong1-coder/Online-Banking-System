package com.obs.backend.feature.account.dto;

import com.obs.backend.feature.account.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

// US-017/US-018: every field is an optional filter on GET /accounts/{accountId}/transactions.
public record TransactionFilter(
        TransactionType type, LocalDate fromDate, LocalDate toDate, BigDecimal minAmount, BigDecimal maxAmount) {
}
