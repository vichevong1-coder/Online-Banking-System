package com.obs.backend.feature.statement.service.impl;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.statement.exception.NoProfileEmailException;
import com.obs.backend.feature.statement.service.StatementService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementServiceImpl implements StatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PdfStatementRenderer pdfStatementRenderer;
    private final JavaMailSender mailSender;

    public StatementServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            PdfStatementRenderer pdfStatementRenderer,
            JavaMailSender mailSender) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.pdfStatementRenderer = pdfStatementRenderer;
        this.mailSender = mailSender;
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

    @Override
    @Transactional(readOnly = true)
    public void emailStatement(UUID userId, UUID accountId, LocalDate fromDate, LocalDate toDate) {
        Account account =
                accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(AccountNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new NoProfileEmailException();
        }

        byte[] pdf = generateStatement(userId, accountId, fromDate, toDate);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("Account Statement - " + account.getAccountNumber());
            helper.setText("Dear " + user.getFirstName() + ",\n\nPlease find attached your account statement for account "
                    + account.getAccountNumber() + ".\n\nThank you for banking with us.");
            helper.addAttachment("statement-" + account.getAccountNumber() + ".pdf", new ByteArrayResource(pdf));
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send statement email", e);
        }
    }
}
