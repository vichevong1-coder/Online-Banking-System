package com.obs.backend.feature.account.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.dto.BalanceResponse;
import com.obs.backend.feature.account.dto.OpenAccountRequest;
import com.obs.backend.feature.account.dto.TransactionFilter;
import com.obs.backend.feature.account.dto.TransactionResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    List<AccountResponse> listAccounts(UUID userId);

    AccountResponse openAccount(UUID userId, OpenAccountRequest request);

    BalanceResponse getBalance(UUID userId, UUID accountId);

    PageResponse<TransactionResponse> listTransactions(
            UUID userId, UUID accountId, TransactionFilter filter, Pageable pageable);
}
