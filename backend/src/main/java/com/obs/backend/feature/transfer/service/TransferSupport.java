package com.obs.backend.feature.transfer.service;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.service.NotificationService;
import com.obs.backend.feature.transfer.config.TransferLimitProperties;
import com.obs.backend.feature.transfer.config.TransferLimitProperties.CurrencyLimits;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import com.obs.backend.feature.transfer.exception.InsufficientFundsException;
import com.obs.backend.feature.transfer.exception.TransferLimitExceededException;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The parts of settlement every way of sending money shares: the US-027 caps,
 * the reference on the receipt, and the US-035 notification.
 *
 * <p>It exists because there are now three paths into the {@code transfers}
 * table — own-accounts (US-025), interbank (US-026) and QR (US-033/US-034) — and
 * a second copy of the limit rules is a copy that can drift. A QR payment being
 * a way around the daily cap is exactly the bug this prevents.
 */
@Component
public class TransferSupport {

    private static final Logger log = LoggerFactory.getLogger(TransferSupport.class);

    private static final String REFERENCE_PREFIX = "TRF-";
    private static final int REFERENCE_BODY_LENGTH = 8;
    // Crockford-ish: no I, L, O, U, so a reference read aloud off a receipt can't
    // be confused with 1 / 0.
    private static final String REFERENCE_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final NotificationService notificationService;
    private final TransferLimitProperties transferLimits;
    private final SecureRandom secureRandom = new SecureRandom();

    public TransferSupport(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            NotificationService notificationService,
            TransferLimitProperties transferLimits) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.notificationService = notificationService;
        this.transferLimits = transferLimits;
    }

    /**
     * US-027, applied identically to every path: per-transfer cap, then the day's
     * running total, then the balance.
     *
     * <p>Funds are checked here rather than left to {@link Account#debit} because
     * that throws an {@code IllegalStateException}, which would surface as a 500
     * where US-027 wants a coded 400.
     */
    public void checkLimitsAndFunds(UUID userId, Account source, BigDecimal amount) {
        CurrencyLimits limits = transferLimits.forCurrency(source.getCurrency());

        if (amount.compareTo(limits.getPerTransfer()) > 0) {
            throw TransferLimitExceededException.perTransfer(limits.getPerTransfer());
        }
        BigDecimal sentToday = sumSentToday(userId, source);
        if (sentToday.add(amount).compareTo(limits.getDaily()) > 0) {
            throw TransferLimitExceededException.daily(limits.getDaily());
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
    }

    /**
     * US-035, for every path: the shared Sprint 3 NotificationService, not a
     * parallel path of its own.
     *
     * <p>Swallowed and logged rather than propagated — the money has already
     * moved by the time this runs, and failing the transfer because the customer
     * could not be told about it would be the worse outcome of the two.
     */
    public void notifyTransferCompleted(UUID userId, String message) {
        try {
            notificationService.sendNotification(userId, "Transfer completed", message, NotificationType.TRANSFER);
        } catch (RuntimeException e) {
            log.warn("Transfer completed but its notification could not be sent for user {}", userId, e);
        }
    }

    /**
     * Short enough to read off a receipt or quote down a phone, random enough not
     * to leak transfer volume the way a sequence would. The uniqueness check
     * backs up the UNIQUE constraint on the column rather than replacing it.
     */
    public String generateReference() {
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

    public Set<UUID> ownedAccountIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        for (Account account : accountRepository.findByUserIdOrderByCreatedAtAsc(userId)) {
            ids.add(account.getId());
        }
        return ids;
    }

    private BigDecimal sumSentToday(UUID userId, Account source) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = startOfDay.plus(Duration.ofDays(1));

        BigDecimal sum = transferRepository.sumSentAmount(
                ownedAccountIds(userId),
                TransferStatus.COMPLETED,
                source.getCurrency(),
                startOfDay,
                startOfNextDay);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
