package com.obs.backend.feature.transfer.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.entity.TransactionType;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.service.NotificationService;
import com.obs.backend.feature.transfer.config.TransferLimitProperties;
import com.obs.backend.feature.transfer.config.TransferLimitProperties.CurrencyLimits;
import com.obs.backend.feature.transfer.dto.CreateTransferRequest;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import com.obs.backend.feature.transfer.exception.CurrencyMismatchException;
import com.obs.backend.feature.transfer.exception.InsufficientFundsException;
import com.obs.backend.feature.transfer.exception.SameAccountTransferException;
import com.obs.backend.feature.transfer.exception.TransferLimitExceededException;
import com.obs.backend.feature.transfer.exception.TransferNotFoundException;
import com.obs.backend.feature.transfer.mapper.TransferMapper;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.transfer.service.TransferService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferServiceImpl implements TransferService {

    /** Amounts are stored as NUMERIC(19,4); everything is normalised to that scale before comparison. */
    private static final int AMOUNT_SCALE = 4;

    private static final String REFERENCE_PREFIX = "TRF-";
    private static final int REFERENCE_BODY_LENGTH = 8;
    // Crockford-ish: no I, L, O, U, so a reference read aloud off a receipt can't
    // be confused with 1 / 0.
    private static final String REFERENCE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final NotificationService notificationService;
    private final TransferLimitProperties transferLimits;
    private final SecureRandom secureRandom = new SecureRandom();

    public TransferServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransferRepository transferRepository,
            TransferMapper transferMapper,
            NotificationService notificationService,
            TransferLimitProperties transferLimits) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.notificationService = notificationService;
        this.transferLimits = transferLimits;
    }

    /**
     * US-025. One transaction writes the transfer row and both ledger legs
     * together — a transfer that left only one leg behind would be a ledger that
     * does not balance, and US-050/US-053 both read the transfer row expecting
     * its legs to exist.
     */
    @Override
    @Transactional
    public TransferResponse transfer(UUID userId, CreateTransferRequest request) {
        // Ownership first, and a 404 either way: an account that isn't the
        // caller's is indistinguishable from one that doesn't exist (Sprint 2).
        Account source = findOwnedAccount(userId, request.fromAccountId());
        Account destination = findOwnedAccount(userId, request.toAccountId());

        if (source.getId().equals(destination.getId())) {
            throw new SameAccountTransferException();
        }
        if (source.getCurrency() != destination.getCurrency()) {
            // Rejected, not converted — there is no rate table (sprint4-todolist.md).
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        CurrencyLimits limits = transferLimits.forCurrency(source.getCurrency());

        if (amount.compareTo(limits.getPerTransfer()) > 0) {
            throw TransferLimitExceededException.perTransfer(limits.getPerTransfer());
        }
        BigDecimal sentToday = sumSentToday(userId, source);
        if (sentToday.add(amount).compareTo(limits.getDaily()) > 0) {
            throw TransferLimitExceededException.daily(limits.getDaily());
        }

        // Checked here rather than letting Account.debit() throw: its
        // IllegalStateException would surface as a 500, and US-027 wants a 400.
        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        Transfer transfer = Transfer.internal(
                source.getId(),
                destination.getId(),
                amount,
                source.getCurrency(),
                generateReference(),
                request.description());
        // saveAndFlush, not save: createdAt is a @CreationTimestamp and is only
        // populated once Hibernate issues the INSERT, so the receipt returned
        // below would otherwise carry a null createdAt. It also gives the legs a
        // transfer id that already exists as a row for their FK.
        transfer = transferRepository.saveAndFlush(transfer);

        source.debit(amount);
        destination.credit(amount);
        accountRepository.save(source);
        accountRepository.save(destination);

        String description = request.description();
        transactionRepository.save(new Transaction(
                source.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                source.getCurrency(),
                description,
                source.getBalance(),
                transfer.getId()));
        transactionRepository.save(new Transaction(
                destination.getId(),
                TransactionType.TRANSFER_IN,
                amount,
                destination.getCurrency(),
                description,
                destination.getBalance(),
                transfer.getId()));

        // US-035 seam: the shared NotificationService, not a parallel path.
        notificationService.sendNotification(
                userId,
                "Transfer completed",
                "%s %s moved from account %s to account %s. Reference %s."
                        .formatted(
                                source.getCurrency(),
                                amount.toPlainString(),
                                source.getAccountNumber(),
                                destination.getAccountNumber(),
                                transfer.getReference()),
                NotificationType.TRANSFER);

        return transferMapper.toResponse(transfer, source.getAccountNumber(), destination.getAccountNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID userId, UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(TransferNotFoundException::new);

        Set<UUID> ownedAccountIds = ownedAccountIds(userId);
        boolean callersOwn = ownedAccountIds.contains(transfer.getFromAccountId())
                || (transfer.getToAccountId() != null && ownedAccountIds.contains(transfer.getToAccountId()));
        if (!callersOwn) {
            // Another customer's transfer: 404, never 403.
            throw new TransferNotFoundException();
        }

        return toResponse(transfer, accountNumbersFor(List.of(transfer)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> listTransfers(UUID userId, Pageable pageable) {
        Set<UUID> ownedAccountIds = ownedAccountIds(userId);
        if (ownedAccountIds.isEmpty()) {
            // An empty IN list is not valid SQL — short-circuit instead.
            return PageResponse.of(Page.<TransferResponse>empty(pageable));
        }

        Page<Transfer> page = transferRepository.findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDesc(
                ownedAccountIds, ownedAccountIds, pageable);
        Map<UUID, String> accountNumbers = accountNumbersFor(page.getContent());
        return PageResponse.of(page.map(transfer -> toResponse(transfer, accountNumbers)));
    }

    private BigDecimal sumSentToday(UUID userId, Account source) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = startOfDay.plus(java.time.Duration.ofDays(1));

        BigDecimal sum = transferRepository.sumSentAmount(
                ownedAccountIds(userId),
                TransferStatus.COMPLETED,
                source.getCurrency(),
                startOfDay,
                startOfNextDay);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private Set<UUID> ownedAccountIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        for (Account account : accountRepository.findByUserIdOrderByCreatedAtAsc(userId)) {
            ids.add(account.getId());
        }
        return ids;
    }

    private Map<UUID, String> accountNumbersFor(List<Transfer> transfers) {
        Set<UUID> ids = new HashSet<>();
        for (Transfer transfer : transfers) {
            ids.add(transfer.getFromAccountId());
            if (transfer.getToAccountId() != null) {
                ids.add(transfer.getToAccountId());
            }
        }
        Map<UUID, String> numbers = new HashMap<>();
        for (Account account : accountRepository.findAllById(ids)) {
            numbers.put(account.getId(), account.getAccountNumber());
        }
        return numbers;
    }

    private TransferResponse toResponse(Transfer transfer, Map<UUID, String> accountNumbers) {
        return transferMapper.toResponse(
                transfer,
                accountNumbers.get(transfer.getFromAccountId()),
                transfer.getToAccountId() == null ? null : accountNumbers.get(transfer.getToAccountId()));
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(AccountNotFoundException::new);
    }

    /**
     * Short enough to read off a receipt or quote down a phone, random enough not
     * to leak transfer volume the way a sequence would. The uniqueness check
     * backs up the UNIQUE constraint on the column rather than replacing it.
     */
    private String generateReference() {
        String candidate;
        do {
            StringBuilder body = new StringBuilder(REFERENCE_BODY_LENGTH);
            for (int i = 0; i < REFERENCE_BODY_LENGTH; i++) {
                body.append(REFERENCE_ALPHABET.charAt(secureRandom.nextInt(REFERENCE_ALPHABET.length())));
            }
            candidate = REFERENCE_PREFIX + body;
        } while (transferRepository.existsByReference(candidate));
        return candidate;
    }
}
