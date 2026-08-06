package com.obs.backend.feature.account.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.dto.BalanceResponse;
import com.obs.backend.feature.account.dto.OpenAccountRequest;
import com.obs.backend.feature.account.dto.TransactionFilter;
import com.obs.backend.feature.account.dto.TransactionResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.mapper.AccountMapper;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.account.service.AccountService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private static final long ACCOUNT_NUMBER_MIN = 100_000_000_000L;
    private static final long ACCOUNT_NUMBER_RANGE = 900_000_000_000L;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts(UUID userId) {
        return accountRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse openAccount(UUID userId, OpenAccountRequest request) {
        Account account = new Account(
                userId, generateAccountNumber(), request.accountType(), request.currency(), BigDecimal.ZERO);
        // saveAndFlush, not save: createdAt is a @CreationTimestamp, populated only when Hibernate
        // actually issues the INSERT. save() alone defers that insert to end-of-transaction, so
        // the entity mapped into the response here would still have a null createdAt.
        return accountMapper.toResponse(accountRepository.saveAndFlush(account));
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId, UUID accountId) {
        return accountMapper.toBalanceResponse(findOwnedAccount(userId, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> listTransactions(
            UUID userId, UUID accountId, TransactionFilter filter, Pageable pageable) {
        Account account = findOwnedAccount(userId, accountId);

        Instant fromInstant =
                filter.fromDate() == null ? null : filter.fromDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = filter.toDate() == null
                ? null
                : filter.toDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.equal(root.get("accountId"), account.getId()));
        if (filter.type() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (fromInstant != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
        }
        if (toInstant != null) {
            // toInstant is the start of the day *after* filter.toDate() — exclusive, so a
            // transaction stamped exactly at that boundary isn't counted as "on" toDate.
            specs.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), toInstant));
        }
        if (filter.minAmount() != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
        }
        if (filter.maxAmount() != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
        }

        Specification<Transaction> combined = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            combined = combined.and(specs.get(i));
        }

        Page<Transaction> page = transactionRepository.findAll(combined, pageable);
        return PageResponse.of(page.map(accountMapper::toResponse));
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(AccountNotFoundException::new);
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            candidate = String.valueOf(ACCOUNT_NUMBER_MIN + secureRandom.nextLong(ACCOUNT_NUMBER_RANGE));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
