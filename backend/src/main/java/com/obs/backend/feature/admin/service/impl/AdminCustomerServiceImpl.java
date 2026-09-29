package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.account.mapper.AccountMapper;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.admin.dto.UpdateCustomerStatusRequest;
import com.obs.backend.feature.admin.exception.CustomerNotFoundException;
import com.obs.backend.feature.admin.mapper.AdminCustomerMapper;
import com.obs.backend.feature.admin.service.AdminCustomerService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.feature.admin.dto.CreateCustomerRequest;
import com.obs.backend.feature.admin.dto.UpdateCustomerRequest;
import com.obs.backend.feature.auth.exception.PhoneAlreadyRegisteredException;
import com.obs.backend.security.AccountStatus;
import com.obs.backend.security.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    private final com.obs.backend.feature.account.repository.TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    private final com.obs.backend.feature.account.service.AccountService accountService;

    public AdminCustomerServiceImpl(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AdminCustomerMapper customerMapper,
            AccountMapper accountMapper,
            com.obs.backend.feature.account.repository.TransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder,
            com.obs.backend.feature.account.service.AccountService accountService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountService = accountService;
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

    // Overrides the class-level readOnly=true. Suspending does not terminate a live session
    // immediately: /auth/refresh re-runs AccountStatusPolicy, so access ends within one access-token
    // lifetime (<=15 min) rather than instantly. Revocation is tracked as hole H9 in architecture.md.
    @Override
    @Transactional
    public CustomerDetailResponse updateStatus(UUID customerId, UpdateCustomerStatusRequest request) {
        User customer = requireCustomer(customerId);
        customer.changeStatus(request.status());
        return customerMapper.toDetail(customer);
    }

    @Override
    @Transactional
    public AccountResponse fundAccount(UUID customerId, UUID accountId, com.obs.backend.feature.admin.dto.FundAccountRequest request) {
        requireCustomer(customerId);
        com.obs.backend.feature.account.entity.Account account = accountRepository.findByIdAndUserId(accountId, customerId)
                .orElseThrow(com.obs.backend.feature.account.exception.AccountNotFoundException::new);
        
        account.credit(request.amount());
        account = accountRepository.saveAndFlush(account);
        
        transactionRepository.save(new com.obs.backend.feature.account.entity.Transaction(
                account.getId(),
                com.obs.backend.feature.account.entity.TransactionType.TRANSFER_IN,
                request.amount(),
                account.getCurrency(),
                "Demo Funding",
                account.getBalance(),
                null // no transfer ID for a direct system fund
        ));
        
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public CustomerDetailResponse createCustomer(CreateCustomerRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new PhoneAlreadyRegisteredException();
        }

        User customer = new User(
                request.firstName(),
                request.lastName(),
                passwordEncoder.encode(request.password() != null ? request.password() : "default123"),
                request.nidNumber(),
                request.nidExpiryDate(),
                request.dateOfBirth(),
                request.gender(),
                request.phone(),
                Role.CUSTOMER,
                AccountStatus.ACTIVE,
                true // Since staff verified it
        );
        userRepository.save(customer);

        // Usually staff would also create an account (e.g. SAVINGS)
        accountService.openAccount(customer.getId(), new com.obs.backend.feature.account.dto.OpenAccountRequest(
                com.obs.backend.feature.account.entity.AccountType.SAVINGS,
                com.obs.backend.feature.account.entity.Currency.USD
        ));

        return customerMapper.toDetail(customer);
    }

    @Override
    @Transactional
    public CustomerDetailResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        User customer = requireCustomer(customerId);

        if (!customer.getPhone().equals(request.phone()) && userRepository.existsByPhone(request.phone())) {
            throw new PhoneAlreadyRegisteredException();
        }

        customer.updateCustomerDetails(
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.nidNumber(),
                request.nidExpiryDate(),
                request.dateOfBirth(),
                request.gender()
        );

        return customerMapper.toDetail(customer);
    }

    private User requireCustomer(UUID customerId) {
        return userRepository
                .findByIdAndRole(customerId, Role.CUSTOMER)
                .orElseThrow(CustomerNotFoundException::new);
    }
}
