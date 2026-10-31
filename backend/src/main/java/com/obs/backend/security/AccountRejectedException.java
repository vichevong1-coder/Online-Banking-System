package com.obs.backend.security;

import org.springframework.security.authentication.AccountStatusException;

/** Thrown by {@link AccountStatusPolicy} when staff rejected the customer's KYC submission. */
public class AccountRejectedException extends AccountStatusException {

    public AccountRejectedException() {
        super("Account registration was rejected");
    }
}
