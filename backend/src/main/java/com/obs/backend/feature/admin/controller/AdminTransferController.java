package com.obs.backend.feature.admin.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.admin.dto.AdminTransferResponse;
import com.obs.backend.feature.admin.service.AdminTransferService;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-050: Admin transfer monitoring feed.
 * Protected with @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/admin/transfers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransferController {

    private final AdminTransferService adminTransferService;

    public AdminTransferController(AdminTransferService adminTransferService) {
        this.adminTransferService = adminTransferService;
    }

    @GetMapping
    public PageResponse<AdminTransferResponse> listTransfers(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Currency currency,
            // One box on the screen, so one parameter: an account id or an account number.
            @RequestParam(required = false) String account,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminTransferService.listTransfers(
                startDate, endDate, minAmount, maxAmount, status, currency, account, pageable);
    }
}
