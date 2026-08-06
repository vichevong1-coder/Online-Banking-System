package com.obs.backend.feature.account.dto;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.account.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        Currency currency,
        String description,
        BigDecimal balanceAfter,
        Instant createdAt) {
}
