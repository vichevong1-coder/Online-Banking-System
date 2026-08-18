package com.obs.backend.feature.admin.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.dto.AccountResponse;
import com.obs.backend.feature.admin.dto.CustomerDetailResponse;
import com.obs.backend.feature.admin.dto.CustomerSummaryResponse;
import com.obs.backend.feature.admin.dto.UpdateCustomerStatusRequest;
import com.obs.backend.feature.admin.service.AdminCustomerService;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
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
        return adminCustomerService.updateStatus(customerId, request);
    }

    @GetMapping("/{customerId}/accounts")
    public List<AccountResponse> listCustomerAccounts(@PathVariable UUID customerId) {
        return adminCustomerService.listCustomerAccounts(customerId);
    }
}
