package com.obs.backend.feature.transfer.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("The source account does not have enough available balance");
    }
}
