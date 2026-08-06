package com.obs.backend.feature.account.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException() {
        super("Account not found");
    }
}
