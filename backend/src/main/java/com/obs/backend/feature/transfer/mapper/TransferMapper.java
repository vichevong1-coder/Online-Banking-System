package com.obs.backend.feature.transfer.mapper;

import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.feature.transfer.entity.Transfer;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    /**
     * Account numbers are passed in rather than looked up here: the receipt shows
     * them, but the transfer row stores only ids and the mapper has no repository.
     * {@code toAccountNumber} is null for an interbank transfer (US-026), which has
     * no local destination account.
     */
    public TransferResponse toResponse(Transfer transfer, String fromAccountNumber, String toAccountNumber) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getReference(),
                transfer.getFromAccountId(),
                fromAccountNumber,
                transfer.getToAccountId(),
                toAccountNumber,
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getCreatedAt());
    }
}
