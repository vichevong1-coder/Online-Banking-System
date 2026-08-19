package com.obs.backend.feature.qr.controller;

import com.obs.backend.feature.qr.dto.QrPayRequest;
import com.obs.backend.feature.qr.dto.QrPayloadResponse;
import com.obs.backend.feature.qr.service.QrService;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qr")
public class QrController {

    private final QrService qrService;
    private final CurrentUserProvider currentUserProvider;

    public QrController(QrService qrService, CurrentUserProvider currentUserProvider) {
        this.qrService = qrService;
        this.currentUserProvider = currentUserProvider;
    }

    /** US-032: the payload for one of the caller's accounts, as a string — the app renders it. */
    @GetMapping("/me")
    public QrPayloadResponse myPayload(@RequestParam UUID accountId) {
        return qrService.myPayload(currentUserProvider.currentUserId(), accountId);
    }

    /**
     * US-033 and US-034: one endpoint for both, because the mobile half is meant
     * to be a camera plus a POST. Whether the scanned payload is a person or a
     * merchant is the server's problem, not the scanner's.
     *
     * <p>Returns the existing {@link TransferResponse}: a QR payment is a
     * transfer, and a response shape of its own would mean US-028's receipt and
     * US-050's feed each needing a second one.
     */
    @PostMapping("/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse pay(@Valid @RequestBody QrPayRequest request) {
        return qrService.pay(currentUserProvider.currentUserId(), request);
    }
}
