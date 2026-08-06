package com.obs.backend.feature.account.dto;

import com.obs.backend.feature.account.entity.Currency;
import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID accountId, String accountNumber, Currency currency, BigDecimal balance) {
}
