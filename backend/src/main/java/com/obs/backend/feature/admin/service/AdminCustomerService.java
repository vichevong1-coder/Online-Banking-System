package com.obs.backend.feature.admin.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.admin.dto.UpdateCustomerStatusRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminCustomerService {

    PageResponse<CustomerSummaryResponse> searchCustomers(String search, Pageable pageable);

    CustomerDetailResponse getCustomer(UUID customerId);

    List<AccountResponse> listCustomerAccounts(UUID customerId);

    CustomerDetailResponse updateStatus(UUID customerId, UpdateCustomerStatusRequest request);

    AccountResponse fundAccount(UUID customerId, UUID accountId, com.obs.backend.feature.admin.dto.FundAccountRequest request);

    CustomerDetailResponse createCustomer(com.obs.backend.feature.admin.dto.CreateCustomerRequest request);

    CustomerDetailResponse updateCustomer(UUID customerId, com.obs.backend.feature.admin.dto.UpdateCustomerRequest request);
}
