package com.obs.backend.feature.statement.service.impl;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.statement.service.StatementService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementServiceImpl implements StatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PdfStatementRenderer pdfStatementRenderer;

    public StatementServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            PdfStatementRenderer pdfStatementRenderer) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.pdfStatementRenderer = pdfStatementRenderer;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStatement(UUID userId, UUID accountId, LocalDate fromDate, LocalDate toDate) {
        Account account =
                accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(AccountNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        LocalDate effectiveFrom = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo = toDate != null ? toDate : LocalDate.now();

        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        account.getId(),
                        effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        effectiveTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        return pdfStatementRenderer.render(user, account, transactions, effectiveFrom, effectiveTo);
    }
}
