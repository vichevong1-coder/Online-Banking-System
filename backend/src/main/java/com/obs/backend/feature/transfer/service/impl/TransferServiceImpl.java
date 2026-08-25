package com.obs.backend.feature.transfer.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.entity.TransactionType;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.transfer.dto.CreateExternalTransferRequest;
import com.obs.backend.feature.transfer.dto.CreateP2pTransferRequest;
import com.obs.backend.feature.transfer.dto.CreateTransferRequest;
import com.obs.backend.feature.transfer.dto.TransferResponse;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.exception.CurrencyMismatchException;
import com.obs.backend.feature.transfer.exception.SameAccountTransferException;
import com.obs.backend.feature.transfer.exception.TransferNotFoundException;
import com.obs.backend.feature.transfer.mapper.TransferMapper;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.transfer.service.TransferService;
import com.obs.backend.feature.transfer.service.TransferSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** US-026: how a destination held at another bank is written into transfers.external_ref. */
    private static final String EXTERNAL_REF_FORMAT = "%s:%s";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final TransferSupport transferSupport;

    public TransferServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransferRepository transferRepository,
            TransferMapper transferMapper,
            TransferSupport transferSupport) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.transferMapper = transferMapper;
        this.transferSupport = transferSupport;
    }

    /**
     * US-025. Both ends must be the caller's; paying someone else's account at
     * this bank is {@link #transferToAccountNumber}. Settlement itself is shared
     * — see {@link #settleInternal}.
     */
    @Override
    @Transactional
    public TransferResponse transfer(UUID userId, CreateTransferRequest request) {
        // Ownership first, and a 404 either way: an account that isn't the
        // caller's is indistinguishable from one that doesn't exist (Sprint 2).
        Account source = findOwnedAccount(userId, request.fromAccountId());
        Account destination = findOwnedAccount(userId, request.toAccountId());

        return settleInternal(userId, source, destination, request.amount(), request.description(), true);
    }

    /**
     * US-026, the same-bank half: money to another customer's account, named by
     * the account number they gave out.
     *
     * <p>Only the <em>source</em> is owner-scoped. The destination deliberately
     * is not — that is the whole point of paying someone else — which is why it
     * is resolved by account number and never by id: an id is not something a
     * payer can know, and accepting one would turn this into an existence oracle
     * for account ids. An unknown number is the same {@code 404
     * ACCOUNT_NOT_FOUND} an unowned account gives, so neither answer tells the
     * caller anything about accounts that are not theirs.
     *
     * <p>Settlement, limits, currency and the ledger legs are US-025's, unchanged
     * — a payment to another person is an internal transfer with someone else on
     * the receiving end, not a different kind of movement.
     */
    @Override
    @Transactional
    public TransferResponse transferToAccountNumber(UUID userId, CreateP2pTransferRequest request) {
        Account source = findOwnedAccount(userId, request.fromAccountId());
        Account destination = accountRepository
                .findByAccountNumber(request.toAccountNumber())
                .orElseThrow(AccountNotFoundException::new);

        return settleInternal(userId, source, destination, request.amount(), request.description(), false);
    }

    /**
     * The shared settlement path for every transfer with both legs inside this
     * bank — US-025's own-accounts move and US-026's payment to another customer.
     * One transaction writes the transfer row and both ledger legs together: a
     * transfer that left only one leg behind would be a ledger that does not
     * balance, and US-050/US-053 both read the transfer row expecting its legs to
     * exist.
     *
     * @param ownAccounts whether both ends belong to the caller — it changes only
     *     the wording of the US-035 notification, never the money.
     */
    private TransferResponse settleInternal(
            UUID userId,
            Account source,
            Account destination,
            BigDecimal requestedAmount,
            String description,
            boolean ownAccounts) {
        if (source.getId().equals(destination.getId())) {
            throw new SameAccountTransferException();
        }
        if (source.getCurrency() != destination.getCurrency()) {
            // Rejected, not converted — there is no rate table (sprint4-todolist.md).
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = requestedAmount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        // US-027, shared with US-026's interbank path and the QR path so there is
        // one set of caps and one place they are enforced.
        transferSupport.checkLimitsAndFunds(userId, source, amount);

        Transfer transfer = Transfer.internal(
                source.getId(),
                destination.getId(),
                amount,
                source.getCurrency(),
                transferSupport.generateReference(),
                description);
        // saveAndFlush, not save: createdAt is a @CreationTimestamp and is only
        // populated once Hibernate issues the INSERT, so the receipt returned
        // below would otherwise carry a null createdAt. It also gives the legs a
        // transfer id that already exists as a row for their FK.
        transfer = transferRepository.saveAndFlush(transfer);

        source.debit(amount);
        destination.credit(amount);
        accountRepository.save(source);
        accountRepository.save(destination);

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

        transferSupport.notifyTransferCompleted(
                userId,
                (ownAccounts
                                ? "%s %s moved from account %s to account %s. Reference %s."
                                : "%s %s sent from account %s to account %s. Reference %s.")
                        .formatted(
                                source.getCurrency(),
                                amount.toPlainString(),
                                source.getAccountNumber(),
                                destination.getAccountNumber(),
                                transfer.getReference()));

        return transferMapper.toResponse(transfer, source.getAccountNumber(), destination.getAccountNumber());
    }

    /**
     * US-026. Interbank, simulated: there is no clearing integration, so the
     * transfer is settled deterministically in-process — accepted, debited, and
     * marked COMPLETED before the method returns. Only the debit leg is written,
     * because the destination account is not ours to credit; that is exactly what
     * {@code transfers.to_account_id NULL} plus {@code external_ref} are for.
     *
     * <p>Every US-027 rule still applies. Currency is the one that differs: with
     * no local destination there is nothing to compare against, so the check
     * becomes "the currency the caller declared must be the source account's" —
     * still a refusal to convert, still {@code 400 CURRENCY_MISMATCH}.
     */
    @Override
    @Transactional
    public TransferResponse transferExternal(UUID userId, CreateExternalTransferRequest request) {
        // 404 for an account that isn't the caller's, same as US-025.
        Account source = findOwnedAccount(userId, request.fromAccountId());

        if (source.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        transferSupport.checkLimitsAndFunds(userId, source, amount);

        String externalRef =
                EXTERNAL_REF_FORMAT.formatted(request.beneficiaryBankCode(), request.beneficiaryAccountNumber());
        // Born PENDING — it is only accepted at this point — then settled below.
        // saveAndFlush for the same reasons as US-025: createdAt and the leg's FK.
        Transfer transfer = Transfer.external(
                source.getId(),
                externalRef,
                amount,
                source.getCurrency(),
                transferSupport.generateReference(),
                request.description());
        transfer = transferRepository.saveAndFlush(transfer);

        source.debit(amount);
        accountRepository.save(source);

        transactionRepository.save(new Transaction(
                source.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                source.getCurrency(),
                request.description(),
                source.getBalance(),
                transfer.getId()));

        // The simulated settlement response. A real integration would leave the
        // row PENDING and complete it on a callback; this is the seam where that
        // would go. Settling here also keeps the transfer inside the US-027 daily
        // cap, which sums COMPLETED rows only.
        transfer.markCompleted();

        transferSupport.notifyTransferCompleted(
                userId,
                "%s %s sent from account %s to %s at bank %s. Reference %s."
                        .formatted(
                                source.getCurrency(),
                                amount.toPlainString(),
                                source.getAccountNumber(),
                                request.beneficiaryAccountNumber(),
                                request.beneficiaryBankCode(),
                                transfer.getReference()));

        return transferMapper.toResponse(transfer, source.getAccountNumber(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID userId, UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(TransferNotFoundException::new);

        Set<UUID> ownedAccountIds = transferSupport.ownedAccountIds(userId);
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
        Set<UUID> ownedAccountIds = transferSupport.ownedAccountIds(userId);
        if (ownedAccountIds.isEmpty()) {
            // An empty IN list is not valid SQL — short-circuit instead.
            return PageResponse.of(Page.<TransferResponse>empty(pageable));
        }

        Page<Transfer> page = transferRepository.findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDesc(
                ownedAccountIds, ownedAccountIds, pageable);
        Map<UUID, String> accountNumbers = accountNumbersFor(page.getContent());
        return PageResponse.of(page.map(transfer -> toResponse(transfer, accountNumbers)));
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
}
