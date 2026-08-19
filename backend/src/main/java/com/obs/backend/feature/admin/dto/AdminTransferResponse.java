package com.obs.backend.feature.admin.dto;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminTransferResponse(
        UUID id,
        String reference,
        UUID fromAccountId,
        String fromAccountNumber,
        UUID toAccountId,
        String toAccountNumber,
        String externalRef,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        String description,
        Instant createdAt) {}
