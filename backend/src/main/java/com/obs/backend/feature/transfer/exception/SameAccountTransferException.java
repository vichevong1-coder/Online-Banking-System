package com.obs.backend.feature.transfer.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Source and destination accounts must be different");
    }
}
