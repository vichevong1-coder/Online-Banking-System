package com.obs.backend.feature.admin.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.admin.dto.CreateCustomerRequest;
import com.obs.backend.feature.admin.dto.UpdateCustomerRequest;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.admin.dto.UpdateCustomerStatusRequest;
import com.obs.backend.feature.admin.service.AdminCustomerService;
import com.obs.backend.feature.audit.AuditService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Class-level guard: every route here is staff-only. JwtAuthenticationFilter grants ROLE_<Role>
// authorities and SecurityConfig has @EnableMethodSecurity, so a CUSTOMER access token is rejected
// before any handler runs. AdminCustomerControllerAccessTest pins that behaviour.
@RestController
@RequestMapping("/admin/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;
    private final AuditService auditService;

    public AdminCustomerController(AdminCustomerService adminCustomerService, AuditService auditService) {
        this.adminCustomerService = adminCustomerService;
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<CustomerSummaryResponse> searchCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return adminCustomerService.searchCustomers(search, pageable);
    }

    @GetMapping("/{customerId}")
    public CustomerDetailResponse getCustomer(@PathVariable UUID customerId) {
        return adminCustomerService.getCustomer(customerId);
    }

    @PatchMapping("/{customerId}/status")
    public CustomerDetailResponse updateStatus(
            @PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        CustomerDetailResponse response = adminCustomerService.updateStatus(customerId, request);
        auditService.logAction("ACCOUNT_STATUS_CHANGED", customerId.toString(), "USER", "Status updated to: " + request.status());
        return response;
    }

    @org.springframework.web.bind.annotation.PostMapping
    public CustomerDetailResponse createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerDetailResponse response = adminCustomerService.createCustomer(request);
        auditService.logAction("CUSTOMER_CREATED", response.id().toString(), "USER", "Staff created customer");
        return response;
    }

    @org.springframework.web.bind.annotation.PutMapping("/{customerId}")
    public CustomerDetailResponse updateCustomer(
            @PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerDetailResponse response = adminCustomerService.updateCustomer(customerId, request);
        auditService.logAction("CUSTOMER_UPDATED", customerId.toString(), "USER", "Staff updated customer details");
        return response;
    }

    @GetMapping("/{customerId}/accounts")
    public List<AccountResponse> listCustomerAccounts(@PathVariable UUID customerId) {
        return adminCustomerService.listCustomerAccounts(customerId);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{customerId}/accounts/{accountId}/fund")
    public AccountResponse fundAccount(
            @PathVariable UUID customerId, 
            @PathVariable UUID accountId, 
            @Valid @RequestBody com.obs.backend.feature.admin.dto.FundAccountRequest request) {
        AccountResponse response = adminCustomerService.fundAccount(customerId, accountId, request);
        auditService.logAction("ACCOUNT_FUNDED", customerId.toString(), "USER", "Funded account " + accountId + " with " + request.amount());
        return response;
    }
}
