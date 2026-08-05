package com.obs.backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

class AccountStatusPolicyTest {

    private final AccountStatusPolicy policy = new AccountStatusPolicy();

    @Test
    void activeAccountIsAllowedToLogIn() {
        assertThatCode(() -> policy.checkLoginAllowed(AccountStatus.ACTIVE)).doesNotThrowAnyException();
    }

    @Test
    void suspendedAccountIsRejectedAsDisabled() {
        assertThatExceptionOfType(DisabledException.class)
                .isThrownBy(() -> policy.checkLoginAllowed(AccountStatus.SUSPENDED));
    }

    @Test
    void lockedAccountIsRejectedAsLocked() {
        assertThatExceptionOfType(LockedException.class)
                .isThrownBy(() -> policy.checkLoginAllowed(AccountStatus.LOCKED));
    }
}
