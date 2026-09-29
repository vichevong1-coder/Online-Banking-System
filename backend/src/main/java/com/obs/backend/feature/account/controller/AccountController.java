package com.obs.backend.feature.account.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.dto.BalanceResponse;
import com.obs.backend.feature.account.dto.OpenAccountRequest;
import com.obs.backend.feature.account.dto.TransactionFilter;
import com.obs.backend.feature.account.dto.TransactionResponse;
import com.obs.backend.feature.account.entity.TransactionType;
import com.obs.backend.feature.account.service.AccountService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;

    public AccountController(AccountService accountService, CurrentUserProvider currentUserProvider) {
        this.accountService = accountService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<AccountResponse> listAccounts() {
        return accountService.listAccounts(currentUserProvider.currentUserId());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return accountService.openAccount(currentUserProvider.currentUserId(), request);
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse getBalance(@PathVariable UUID accountId) {
        return accountService.getBalance(currentUserProvider.currentUserId(), accountId);
    }

    @GetMapping("/{accountId}/transactions")
    public PageResponse<TransactionResponse> listTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TransactionFilter filter = new TransactionFilter(type, fromDate, toDate, minAmount, maxAmount);
        Pageable pageable = PageRequest.of(page, size);
        return accountService.listTransactions(currentUserProvider.currentUserId(), accountId, filter, pageable);
    }

    @GetMapping("/lookup")
    public com.obs.backend.feature.account.dto.AccountLookupResponse lookupAccount(@RequestParam String number) {
        return accountService.lookupAccount(number);
    }
}
