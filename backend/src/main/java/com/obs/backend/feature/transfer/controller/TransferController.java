package com.obs.backend.feature.transfer.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.transfer.dto.CreateTransferRequest;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.feature.transfer.service.TransferService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;
    private final CurrentUserProvider currentUserProvider;

    public TransferController(TransferService transferService, CurrentUserProvider currentUserProvider) {
        this.transferService = transferService;
        this.currentUserProvider = currentUserProvider;
    }

    /** US-025: between the caller's own accounts. Interbank (US-026) is a separate endpoint, not yet built. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        return transferService.transfer(currentUserProvider.currentUserId(), request);
    }

    /** US-028: the caller's own transfer history, newest first. */
    @GetMapping
    public PageResponse<TransferResponse> listTransfers(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferService.listTransfers(currentUserProvider.currentUserId(), pageable);
    }

    /** US-028: receipt. */
    @GetMapping("/{transferId}")
    public TransferResponse getTransfer(@PathVariable UUID transferId) {
        return transferService.getTransfer(currentUserProvider.currentUserId(), transferId);
    }
}
