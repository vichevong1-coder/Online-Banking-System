package com.obs.backend.feature.qr.service.impl;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.entity.TransactionType;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.qr.dto.QrPayRequest;
import com.obs.backend.feature.qr.dto.QrPayloadResponse;
import com.obs.backend.feature.qr.entity.Merchant;
import com.obs.backend.feature.qr.exception.MerchantDeclinedException;
import com.obs.backend.feature.qr.exception.QrAmountMismatchException;
import com.obs.backend.feature.qr.exception.QrAmountRequiredException;
import com.obs.backend.feature.qr.exception.QrTargetNotFoundException;
import com.obs.backend.feature.qr.repository.MerchantRepository;
import com.obs.backend.feature.qr.service.QrPayload;
import com.obs.backend.feature.qr.service.QrService;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.exception.CurrencyMismatchException;
import com.obs.backend.feature.transfer.exception.SameAccountTransferException;
import com.obs.backend.feature.transfer.mapper.TransferMapper;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.transfer.service.TransferSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrServiceImpl implements QrService {

    /** Amounts are stored as NUMERIC(19,4); everything is normalised to that scale before comparison. */
    private static final int AMOUNT_SCALE = 4;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final MerchantRepository merchantRepository;
    private final TransferMapper transferMapper;
    private final TransferSupport transferSupport;

    public QrServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransferRepository transferRepository,
            MerchantRepository merchantRepository,
            TransferMapper transferMapper,
            TransferSupport transferSupport) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.merchantRepository = merchantRepository;
        this.transferMapper = transferMapper;
        this.transferSupport = transferSupport;
    }

    /**
     * US-032. Owner-scoped like everything else: an account the caller does not
     * own is a 404, never a 403.
     */
    @Override
    @Transactional(readOnly = true)
    public QrPayloadResponse myPayload(UUID userId, UUID accountId) {
        Account account = findOwnedAccount(userId, accountId);
        return new QrPayloadResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCurrency(),
                QrPayload.forAccountNumber(account.getAccountNumber()));
    }

    /**
     * US-033 and US-034 in one method, because they are one endpoint: a merchant
     * payment differs from a person-to-person one only in what the payload
     * resolves to, and splitting them would force the mobile app to parse the
     * scanned string to know where to POST it.
     *
     * <p>The same {@code @Transactional} settlement shape as US-025 — transfer
     * row and both ledger legs together, or neither.
     *
     * <p>{@code noRollbackFor} is what makes the decline path work: a declined
     * payment must leave a FAILED transfer row behind, and the exception that
     * carries the 400 back would otherwise take that row down with it. Nothing
     * else has been written by that point, so committing is exactly right.
     */
    @Override
    @Transactional(noRollbackFor = MerchantDeclinedException.class)
    public TransferResponse pay(UUID userId, QrPayRequest request) {
        QrPayload payload = QrPayload.parse(request.payload());

        // Merchant or person, the destination is resolved from the payload on
        // every call rather than trusted from the scan.
        Merchant merchant = resolveMerchant(payload);
        Account destination = resolveDestination(payload, merchant);
        // The source is still the caller's own — only the destination is not.
        Account source = findOwnedAccount(userId, request.fromAccountId());

        if (source.getId().equals(destination.getId())) {
            // Scanning your own code is a mis-scan, not a payment.
            throw new SameAccountTransferException();
        }

        BigDecimal amount = reconcileAmount(payload, request);
        Currency currency = payload.hasAmount() ? payload.currency() : request.currency();

        if (merchant != null && merchant.declinesEverything()) {
            return declineForMerchant(source, destination, merchant, amount, currency, request.description());
        }

        if (source.getCurrency() != currency || source.getCurrency() != destination.getCurrency()) {
            // Rejected, not converted — there is no rate table (sprint4-todolist.md).
            throw new CurrencyMismatchException();
        }

        // US-027 unchanged: a QR payment is a transfer and shares the daily cap
        // with US-025 and US-026 rather than being a side channel around it.
        transferSupport.checkLimitsAndFunds(userId, source, amount);

        // saveAndFlush, not save: createdAt is a @CreationTimestamp and is only
        // populated once Hibernate issues the INSERT, and the legs need a
        // transfer id that already exists as a row for their FK.
        Transfer transfer = Transfer.internal(
                source.getId(),
                destination.getId(),
                amount,
                source.getCurrency(),
                transferSupport.generateReference(),
                request.description());
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

        String payee = merchant == null
                ? "account " + destination.getAccountNumber()
                : merchant.getDisplayName();
        transferSupport.notifyTransferCompleted(
                userId,
                "%s %s paid to %s by QR from account %s. Reference %s."
                        .formatted(
                                source.getCurrency(),
                                amount.toPlainString(),
                                payee,
                                source.getAccountNumber(),
                                transfer.getReference()));

        return transferMapper.toResponse(transfer, source.getAccountNumber(), destination.getAccountNumber());
    }

    /**
     * US-034's failure path. The FAILED row is written first and no balance is
     * touched at all, so the decline shows up in US-028's history and US-050's
     * feed while the payer's balance is exactly what it was.
     *
     * <p>Never returns — the return type only lets the caller write it as a
     * {@code return}, keeping the settlement path below unindented.
     */
    private TransferResponse declineForMerchant(
            Account source,
            Account destination,
            Merchant merchant,
            BigDecimal amount,
            Currency currency,
            String description) {
        Transfer declined = Transfer.internal(
                source.getId(),
                destination.getId(),
                amount,
                currency,
                transferSupport.generateReference(),
                description);
        declined.markFailed();
        transferRepository.saveAndFlush(declined);

        throw new MerchantDeclinedException(merchant.getDisplayName());
    }

    /** Null for a personal payload; a merchant payload that names nothing is a rejected scan. */
    private Merchant resolveMerchant(QrPayload payload) {
        return switch (payload.type()) {
            case P -> null;
            case M -> merchantRepository
                    .findByMerchantCode(payload.target())
                    .orElseThrow(QrTargetNotFoundException::new);
        };
    }

    private Account resolveDestination(QrPayload payload, Merchant merchant) {
        // A merchant settles into an ordinary account, so both payload types end
        // up as the same kind of internal transfer.
        UUID settlementAccountId = merchant == null ? null : merchant.getSettlementAccountId();
        return switch (payload.type()) {
            case P -> accountRepository
                    .findByAccountNumber(payload.target())
                    .orElseThrow(QrTargetNotFoundException::new);
            case M -> accountRepository.findById(settlementAccountId).orElseThrow(QrTargetNotFoundException::new);
        };
    }

    /**
     * A payload that fixes its amount may only be agreed with — that is what
     * stops a merchant's displayed price being quietly underpaid. One that does
     * not carries no price at all, so the request has to name one.
     */
    private BigDecimal reconcileAmount(QrPayload payload, QrPayRequest request) {
        if (payload.hasAmount()) {
            BigDecimal fixed = payload.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
            if (request.amount() == null && request.currency() == null) {
                return fixed;
            }
            BigDecimal requested = request.amount() == null
                    ? null
                    : request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
            boolean amountAgrees = requested == null || requested.compareTo(fixed) == 0;
            boolean currencyAgrees = request.currency() == null || request.currency() == payload.currency();
            if (!amountAgrees || !currencyAgrees) {
                throw new QrAmountMismatchException();
            }
            return fixed;
        }

        if (request.amount() == null || request.currency() == null) {
            throw new QrAmountRequiredException();
        }
        return request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }

    private Account findOwnedAccount(UUID userId, UUID accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId).orElseThrow(AccountNotFoundException::new);
    }
}
