package com.obs.backend.feature.account.dto;

import com.obs.backend.feature.account.entity.AccountType;
import com.obs.backend.feature.account.entity.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        Currency currency,
        BigDecimal balance,
        Instant createdAt) {
}
