package com.obs.backend.feature.bill.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Transaction;
import com.obs.backend.feature.account.entity.TransactionType;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.account.repository.TransactionRepository;
import com.obs.backend.feature.bill.dto.BillPaymentResponse;
import com.obs.backend.feature.bill.dto.CreateRecurringBillRequest;
import com.obs.backend.feature.bill.dto.PayBillRequest;
import com.obs.backend.feature.bill.dto.RecurringBillPaymentResponse;
import com.obs.backend.feature.bill.entity.BillPayment;
import com.obs.backend.feature.bill.entity.BillProvider;
import com.obs.backend.feature.bill.entity.RecurringBillPayment;
import com.obs.backend.feature.bill.exception.BillPaymentNotFoundException;
import com.obs.backend.feature.bill.exception.BillProviderNotFoundException;
import com.obs.backend.feature.bill.exception.InvalidBillAccountNumberException;
import com.obs.backend.feature.bill.exception.RecurringBillNotFoundException;
import com.obs.backend.feature.bill.repository.BillPaymentRepository;
import com.obs.backend.feature.bill.repository.BillProviderRepository;
import com.obs.backend.feature.bill.repository.RecurringBillPaymentRepository;
import com.obs.backend.feature.bill.service.BillPaymentService;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.exception.CurrencyMismatchException;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.transfer.service.TransferSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillPaymentServiceImpl implements BillPaymentService {

    private static final int AMOUNT_SCALE = 4;

    private final AccountRepository accountRepository;
    private final BillProviderRepository billProviderRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final RecurringBillPaymentRepository recurringBillPaymentRepository;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final TransferSupport transferSupport;

    public BillPaymentServiceImpl(
            AccountRepository accountRepository,
            BillProviderRepository billProviderRepository,
            BillPaymentRepository billPaymentRepository,
            RecurringBillPaymentRepository recurringBillPaymentRepository,
            TransferRepository transferRepository,
            TransactionRepository transactionRepository,
            TransferSupport transferSupport) {
        this.accountRepository = accountRepository;
        this.billProviderRepository = billProviderRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.recurringBillPaymentRepository = recurringBillPaymentRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
        this.transferSupport = transferSupport;
    }

    @Override
    @Transactional
    public BillPaymentResponse payBill(UUID userId, PayBillRequest request) {
        Account source = accountRepository.findByIdAndUserId(request.fromAccountId(), userId)
                .orElseThrow(AccountNotFoundException::new);

        BillProvider provider = billProviderRepository.findById(request.providerId())
                .orElseThrow(BillProviderNotFoundException::new);

        if (!provider.isActive()) {
            throw new BillProviderNotFoundException();
        }

        if (provider.getAccountNumberPattern() != null && !provider.getAccountNumberPattern().isBlank()) {
            if (!request.billAccountNumber().matches(provider.getAccountNumberPattern())) {
                throw new InvalidBillAccountNumberException();
            }
        }

        if (source.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        transferSupport.checkLimitsAndFunds(userId, source, amount);

        String externalRef = "BILL:%s:%s".formatted(provider.getName(), request.billAccountNumber());

        Transfer transfer = Transfer.external(
                source.getId(),
                externalRef,
                amount,
                source.getCurrency(),
                transferSupport.generateReference(),
                "Bill payment to " + provider.getName()
        );
        transfer = transferRepository.saveAndFlush(transfer);

        source.debit(amount);
        accountRepository.save(source);

        transactionRepository.save(new Transaction(
                source.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                source.getCurrency(),
                "Bill payment to %s (%s)".formatted(provider.getName(), request.billAccountNumber()),
                source.getBalance(),
                transfer.getId()
        ));

        transfer.markCompleted();

        BillPayment billPayment = new BillPayment(
                userId,
                provider.getId(),
                source.getId(),
                request.billAccountNumber(),
                amount,
                source.getCurrency(),
                transfer.getId(),
                transfer.getReference(),
                "COMPLETED"
        );
        billPayment = billPaymentRepository.save(billPayment);

        transferSupport.notifyTransferCompleted(
                userId,
                "%s %s paid to %s for bill account %s. Reference %s."
                        .formatted(
                                source.getCurrency(),
                                amount.toPlainString(),
                                provider.getName(),
                                request.billAccountNumber(),
                                transfer.getReference()
                        )
        );

        return toResponse(billPayment, provider, source);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BillPaymentResponse> getPaymentHistory(UUID userId, Pageable pageable) {
        Page<BillPayment> page = billPaymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BillPaymentResponse getPaymentReceipt(UUID userId, UUID paymentId) {
        BillPayment payment = billPaymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(BillPaymentNotFoundException::new);
        return toResponse(payment);
    }

    @Override
    @Transactional
    public RecurringBillPaymentResponse createRecurring(UUID userId, CreateRecurringBillRequest request) {
        Account source = accountRepository.findByIdAndUserId(request.fromAccountId(), userId)
                .orElseThrow(AccountNotFoundException::new);

        BillProvider provider = billProviderRepository.findById(request.providerId())
                .orElseThrow(BillProviderNotFoundException::new);

        if (!provider.isActive()) {
            throw new BillProviderNotFoundException();
        }

        if (provider.getAccountNumberPattern() != null && !provider.getAccountNumberPattern().isBlank()) {
            if (!request.billAccountNumber().matches(provider.getAccountNumberPattern())) {
                throw new InvalidBillAccountNumberException();
            }
        }

        if (source.getCurrency() != request.currency()) {
            throw new CurrencyMismatchException();
        }

        BigDecimal amount = request.amount().setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);

        RecurringBillPayment recurring = new RecurringBillPayment(
                userId,
                provider.getId(),
                source.getId(),
                request.billAccountNumber(),
                amount,
                source.getCurrency(),
                request.frequency(),
                request.nextPaymentDate()
        );
        recurring = recurringBillPaymentRepository.save(recurring);
        return toResponse(recurring, provider, source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringBillPaymentResponse> getRecurring(UUID userId) {
        return recurringBillPaymentRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelRecurring(UUID userId, UUID recurringId) {
        RecurringBillPayment recurring = recurringBillPaymentRepository.findByIdAndUserId(recurringId, userId)
                .orElseThrow(RecurringBillNotFoundException::new);
        recurring.cancel();
        recurringBillPaymentRepository.save(recurring);
    }

    private BillPaymentResponse toResponse(BillPayment p) {
        BillProvider provider = billProviderRepository.findById(p.getProviderId()).orElse(null);
        Account account = accountRepository.findById(p.getAccountId()).orElse(null);
        return toResponse(p, provider, account);
    }

    private BillPaymentResponse toResponse(BillPayment p, BillProvider provider, Account account) {
        return new BillPaymentResponse(
                p.getId(),
                p.getProviderId(),
                provider != null ? provider.getName() : "Unknown Provider",
                provider != null ? provider.getCategory() : null,
                p.getAccountId(),
                account != null ? account.getAccountNumber() : "Unknown Account",
                p.getBillAccountNumber(),
                p.getAmount(),
                p.getCurrency(),
                p.getTransferId(),
                p.getReference(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }

    private RecurringBillPaymentResponse toResponse(RecurringBillPayment r) {
        BillProvider provider = billProviderRepository.findById(r.getProviderId()).orElse(null);
        Account account = accountRepository.findById(r.getAccountId()).orElse(null);
        return toResponse(r, provider, account);
    }

    private RecurringBillPaymentResponse toResponse(RecurringBillPayment r, BillProvider provider, Account account) {
        return new RecurringBillPaymentResponse(
                r.getId(),
                r.getProviderId(),
                provider != null ? provider.getName() : "Unknown Provider",
                provider != null ? provider.getCategory() : null,
                r.getAccountId(),
                account != null ? account.getAccountNumber() : "Unknown Account",
                r.getBillAccountNumber(),
                r.getAmount(),
                r.getCurrency(),
                r.getFrequency(),
                r.getNextPaymentDate(),
                r.isActive(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
