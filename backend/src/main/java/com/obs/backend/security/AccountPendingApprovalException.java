package com.obs.backend.security;

import org.springframework.security.authentication.AccountStatusException;

/** Thrown by {@link AccountStatusPolicy} when staff have not yet approved a new customer's KYC. */
public class AccountPendingApprovalException extends AccountStatusException {

    public AccountPendingApprovalException() {
        super("Account is awaiting approval");
    }
}
