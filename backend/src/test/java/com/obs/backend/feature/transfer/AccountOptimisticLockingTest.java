package com.obs.backend.feature.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.AccountType;
import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.security.Role;
import com.obs.backend.feature.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * US-027 prerequisite: {@code accounts.balance} is guarded by a {@code @Version}
 * column, so two debits racing on the same account cannot both succeed.
 *
 * <p>Deliberately <b>not</b> {@code @Transactional} — the whole point is two
 * separate transactions reading the same row. Wrapping the test in one
 * transaction would give both reads the same managed instance and the conflict
 * would never arise. Rows are cleaned up explicitly instead.
 *
 * <p>The two reads are sequenced rather than threaded so the test is
 * deterministic: a real thread race reproduces the lost update only
 * intermittently, which makes for a test that passes for the wrong reason.
 * The stale copy here is exactly what the losing thread would hold.
 */
@SpringBootTest
class AccountOptimisticLockingTest {

    @Autowired private AccountRepository accountRepository;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void createFundedAccount() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        User owner = userRepository.save(User.createStaff(
                "Lock", "Test", "lock-" + unique + "@example.com", "+855-99-" + unique, "hash", Role.ADMIN));
        userId = owner.getId();

        Account account = accountRepository.save(new Account(
                userId, "LOCK" + unique, AccountType.SAVINGS, Currency.USD, new BigDecimal("100.0000")));
        accountId = account.getId();
    }

    @AfterEach
    void removeFundedAccount() {
        accountRepository.deleteById(accountId);
        userRepository.deleteById(userId);
    }

    @Test
    void aNewAccountStartsAtVersionZero() {
        // Guards the V9 migration's NOT NULL DEFAULT 0: a null version on an
        // existing row makes Hibernate throw at the first write, not at migrate
        // time, so every seeded demo account would fail on its first transfer.
        assertThat(accountRepository.findById(accountId)).isPresent();
        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("100.0000");
    }

    @Test
    void concurrentDebitsCannotBothSucceedAndOverdrawTheAccount() {
        // Two transactions read the same row. Each read happens outside a
        // transaction, so each returns its own detached instance at version 0 —
        // the same state two racing threads would each be holding.
        Account first = accountRepository.findById(accountId).orElseThrow();
        Account second = accountRepository.findById(accountId).orElseThrow();

        // The winner debits 80 of the 100 available and bumps the version to 1.
        first.debit(new BigDecimal("80.0000"));
        accountRepository.save(first);

        // The loser is still holding version 0. Without @Version this write
        // would land, leaving a balance of 20 from a starting 100 after 160 was
        // withdrawn — money created out of nothing.
        second.debit(new BigDecimal("80.0000"));
        assertThatThrownBy(() -> accountRepository.saveAndFlush(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Exactly one debit survived.
        Account settled = accountRepository.findById(accountId).orElseThrow();
        assertThat(settled.getBalance()).isEqualByComparingTo("20.0000");
    }

    @Test
    void debitingMoreThanTheBalanceIsRejected() {
        Account account = accountRepository.findById(accountId).orElseThrow();

        assertThatThrownBy(() -> account.debit(new BigDecimal("100.0001")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");

        assertThat(account.getBalance()).isEqualByComparingTo("100.0000");
    }
}
