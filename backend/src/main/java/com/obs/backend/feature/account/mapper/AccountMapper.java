package com.obs.backend.feature.account.mapper;

import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.dto.BalanceResponse;
import com.obs.backend.feature.account.dto.TransactionResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt());
    }

    public BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
                account.getId(), account.getAccountNumber(), account.getCurrency(), account.getBalance());
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt());
    }
}
