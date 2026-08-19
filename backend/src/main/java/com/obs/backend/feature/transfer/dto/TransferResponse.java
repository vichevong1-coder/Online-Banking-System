package com.obs.backend.feature.transfer.dto;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** US-028 receipt payload, also used for each row of the caller's history. */
public record TransferResponse(
        UUID id,
        String reference,
        UUID fromAccountId,
        String fromAccountNumber,
        UUID toAccountId,
        String toAccountNumber,
        // US-026: "BANKCODE:ACCOUNTNUMBER" at the receiving bank. Null for an
        // internal transfer, which names its destination with the two fields above.
        String externalRef,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        String description,
        Instant createdAt) {
}
