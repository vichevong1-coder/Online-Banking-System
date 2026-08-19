package com.obs.backend.feature.transfer.exception;

/**
 * Also thrown when the transfer exists but neither leg belongs to the caller —
 * 404 rather than 403, the Sprint 2 rule, so ids cannot probe ownership.
 */
public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException() {
        super("Transfer not found");
    }
}
