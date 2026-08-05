package com.obs.backend.security;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

/**
 * The single place a login path (customer or admin) checks whether an account's status permits
 * login. Every login story reuses this rather than re-deriving the status → outcome mapping.
 */
@Service
public class AccountStatusPolicy {

    public void checkLoginAllowed(AccountStatus status) {
        switch (status) {
            case ACTIVE -> {
            }
            case SUSPENDED -> throw new DisabledException("Account is suspended");
            case LOCKED -> throw new LockedException("Account is locked");
        }
    }
}
