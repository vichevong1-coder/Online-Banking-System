package com.obs.backend.feature.transfer.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.transfer.dto.CreateTransferRequest;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    /** US-025: moves money between two accounts the caller owns. */
    TransferResponse transfer(UUID userId, CreateTransferRequest request);

    /** US-028: the receipt for one of the caller's own transfers. */
    TransferResponse getTransfer(UUID userId, UUID transferId);

    /** US-028: the caller's own transfer history, newest first. */
    PageResponse<TransferResponse> listTransfers(UUID userId, Pageable pageable);
}
