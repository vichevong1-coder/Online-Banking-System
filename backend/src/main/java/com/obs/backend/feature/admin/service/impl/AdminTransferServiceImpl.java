package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.admin.dto.AdminTransferResponse;
import com.obs.backend.feature.admin.service.AdminTransferService;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import com.obs.backend.feature.transfer.repository.TransferRepository;
import com.obs.backend.feature.transfer.repository.TransferSpecification;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTransferServiceImpl implements AdminTransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    public AdminTransferServiceImpl(
            TransferRepository transferRepository,
            AccountRepository accountRepository) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminTransferResponse> listTransfers(
            String fromDate,
            String startDate,
            String toDate,
            String endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            TransferStatus status,
            Currency currency,
            String accountNumber,
            UUID accountId,
            String account,
            Pageable pageable) {

        String effectiveFrom = fromDate != null ? fromDate : startDate;
        String effectiveTo = toDate != null ? toDate : endDate;

        ParsedDate fromParsed = parseFromDate(effectiveFrom);
        ParsedDate toParsed = parseToDate(effectiveTo);

        UUID effectiveAccountId = accountId;
        String effectiveAccountNumber = accountNumber;

        if (account != null && !account.isBlank()) {
            account = account.trim();
            try {
                effectiveAccountId = UUID.fromString(account);
            } catch (IllegalArgumentException e) {
                effectiveAccountNumber = account;
            }
        }

        List<UUID> matchingAccountIds = new ArrayList<>();
        String externalRefPattern = null;

        if (effectiveAccountNumber != null && !effectiveAccountNumber.isBlank()) {
            effectiveAccountNumber = effectiveAccountNumber.trim();
            Optional<Account> foundAccount = accountRepository.findByAccountNumber(effectiveAccountNumber);
            foundAccount.ifPresent(acc -> matchingAccountIds.add(acc.getId()));
            externalRefPattern = effectiveAccountNumber;
        }

        Specification<Transfer> spec = TransferSpecification.filterBy(
                fromParsed != null ? fromParsed.instant() : null,
                toParsed != null ? toParsed.instant() : null,
                toParsed != null && toParsed.inclusive(),
                minAmount,
                maxAmount,
                status,
                currency,
                effectiveAccountId,
                matchingAccountIds,
                externalRefPattern);

        Page<Transfer> page = transferRepository.findAll(spec, pageable);
        Map<UUID, String> accountNumbers = accountNumbersFor(page.getContent());

        return PageResponse.of(page.map(transfer -> toResponse(transfer, accountNumbers)));
    }

    private Map<UUID, String> accountNumbersFor(List<Transfer> transfers) {
        Set<UUID> ids = new HashSet<>();
        for (Transfer transfer : transfers) {
            if (transfer.getFromAccountId() != null) {
                ids.add(transfer.getFromAccountId());
            }
            if (transfer.getToAccountId() != null) {
                ids.add(transfer.getToAccountId());
            }
        }
        Map<UUID, String> numbers = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Account account : accountRepository.findAllById(ids)) {
                numbers.put(account.getId(), account.getAccountNumber());
            }
        }
        return numbers;
    }

    private AdminTransferResponse toResponse(Transfer transfer, Map<UUID, String> accountNumbers) {
        return new AdminTransferResponse(
                transfer.getId(),
                transfer.getReference(),
                transfer.getFromAccountId(),
                accountNumbers.get(transfer.getFromAccountId()),
                transfer.getToAccountId(),
                transfer.getToAccountId() == null ? null : accountNumbers.get(transfer.getToAccountId()),
                transfer.getExternalRef(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getDescription(),
                transfer.getCreatedAt());
    }

    private record ParsedDate(Instant instant, boolean inclusive) {}

    private ParsedDate parseFromDate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        input = input.trim();
        if (input.contains("T")) {
            try {
                return new ParsedDate(Instant.parse(input), true);
            } catch (DateTimeParseException e) {
                return new ParsedDate(OffsetDateTime.parse(input).toInstant(), true);
            }
        }
        return new ParsedDate(LocalDate.parse(input).atStartOfDay(ZoneOffset.UTC).toInstant(), true);
    }

    private ParsedDate parseToDate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        input = input.trim();
        if (input.contains("T")) {
            try {
                return new ParsedDate(Instant.parse(input), true);
            } catch (DateTimeParseException e) {
                return new ParsedDate(OffsetDateTime.parse(input).toInstant(), true);
            }
        }
        return new ParsedDate(LocalDate.parse(input).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), false);
    }
}
