package com.obs.backend.feature.admin.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.admin.dto.AdminTransferResponse;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AdminTransferService {

    PageResponse<AdminTransferResponse> listTransfers(
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
            Pageable pageable);
}
