package com.obs.backend.feature.admin.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminCustomerService {

    PageResponse<CustomerSummaryResponse> searchCustomers(String search, Pageable pageable);

    CustomerDetailResponse getCustomer(UUID customerId);

    List<AccountResponse> listCustomerAccounts(UUID customerId);
}
