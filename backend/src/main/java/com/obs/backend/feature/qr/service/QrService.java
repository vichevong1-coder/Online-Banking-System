package com.obs.backend.feature.qr.service;

import com.obs.backend.feature.qr.dto.QrPayRequest;
import com.obs.backend.feature.qr.dto.QrPayloadResponse;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import java.util.UUID;

public interface QrService {

    /** US-032: the payload one of the caller's own accounts is represented by. */
    QrPayloadResponse myPayload(UUID userId, UUID accountId);

    /** US-033/US-034: resolves a scanned payload — personal or merchant — and settles it. */
    TransferResponse pay(UUID userId, QrPayRequest request);
}
