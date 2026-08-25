package com.obs.backend.feature.card.dto;

import com.obs.backend.feature.card.entity.CardStatus;
import com.obs.backend.feature.card.entity.CardType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID accountId,
        String accountNumber,
        String cardHolderName,
        String cardNumberMasked,
        String cardNumberLastFour,
        CardType cardType,
        CardStatus status,
        String expiryDate,
        BigDecimal dailyLimit,
        BigDecimal perTransactionLimit,
        Instant createdAt,
        Instant updatedAt
) {}
