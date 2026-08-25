package com.obs.backend.feature.bill.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.bill.dto.BillPaymentResponse;
import com.obs.backend.feature.bill.dto.CreateRecurringBillRequest;
import com.obs.backend.feature.bill.dto.PayBillRequest;
import com.obs.backend.feature.bill.dto.RecurringBillPaymentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface BillPaymentService {
    BillPaymentResponse payBill(UUID userId, PayBillRequest request);
    PageResponse<BillPaymentResponse> getPaymentHistory(UUID userId, Pageable pageable);
    BillPaymentResponse getPaymentReceipt(UUID userId, UUID paymentId);
    RecurringBillPaymentResponse createRecurring(UUID userId, CreateRecurringBillRequest request);
    List<RecurringBillPaymentResponse> getRecurring(UUID userId);
    void cancelRecurring(UUID userId, UUID recurringId);
}
