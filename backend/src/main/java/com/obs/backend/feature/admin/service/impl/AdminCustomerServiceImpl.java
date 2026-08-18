package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.mapper.AccountMapper;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.admin.exception.CustomerNotFoundException;
import com.obs.backend.feature.admin.mapper.AdminCustomerMapper;
import com.obs.backend.feature.admin.service.AdminCustomerService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AdminCustomerMapper customerMapper;
    private final AccountMapper accountMapper;

    public AdminCustomerServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AdminCustomerMapper customerMapper,
            AccountMapper accountMapper) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    public PageResponse<CustomerSummaryResponse> searchCustomers(String search, Pageable pageable) {
        // Blank and absent search mean the same thing — the whole list, which the query expresses as
        // an empty needle ('%%' matches every row). Never pass null: Postgres cannot infer the type
        // of a null String parameter and LOWER() then fails against bytea.
        String normalized = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(
                userRepository.searchByRole(Role.CUSTOMER, normalized, pageable).map(customerMapper::toSummary));
    }

    @Override
    public CustomerDetailResponse getCustomer(UUID customerId) {
        return customerMapper.toDetail(requireCustomer(customerId));
    }

    @Override
    public List<AccountResponse> listCustomerAccounts(UUID customerId) {
        // Resolve the customer first so a staff id or an unknown id 404s, rather than returning an
        // empty account list that reads as "this customer has no accounts".
        User customer = requireCustomer(customerId);
        return accountRepository.findByUserIdOrderByCreatedAtAsc(customer.getId()).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    private User requireCustomer(UUID customerId) {
        return userRepository
                .findByIdAndRole(customerId, Role.CUSTOMER)
                .orElseThrow(CustomerNotFoundException::new);
    }
}
