package com.obs.backend.feature.bill.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.bill.dto.BillPaymentResponse;
import com.obs.backend.feature.bill.dto.CreateRecurringBillRequest;
import com.obs.backend.feature.bill.dto.PayBillRequest;
import com.obs.backend.feature.bill.dto.RecurringBillPaymentResponse;
import com.obs.backend.feature.bill.service.BillPaymentService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bill-payments")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final CurrentUserProvider currentUserProvider;

    public BillPaymentController(
            BillPaymentService billPaymentService,
            CurrentUserProvider currentUserProvider) {
        this.billPaymentService = billPaymentService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillPaymentResponse payBill(@Valid @RequestBody PayBillRequest request) {
        return billPaymentService.payBill(currentUserProvider.currentUserId(), request);
    }

    @GetMapping
    public PageResponse<BillPaymentResponse> getPaymentHistory(Pageable pageable) {
        return billPaymentService.getPaymentHistory(currentUserProvider.currentUserId(), pageable);
    }

    @GetMapping("/{id}")
    public BillPaymentResponse getPaymentReceipt(@PathVariable UUID id) {
        return billPaymentService.getPaymentReceipt(currentUserProvider.currentUserId(), id);
    }

    @PostMapping("/recurring")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringBillPaymentResponse createRecurring(@Valid @RequestBody CreateRecurringBillRequest request) {
        return billPaymentService.createRecurring(currentUserProvider.currentUserId(), request);
    }

    @GetMapping("/recurring")
    public List<RecurringBillPaymentResponse> getRecurring() {
        return billPaymentService.getRecurring(currentUserProvider.currentUserId());
    }

    @DeleteMapping("/recurring/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRecurring(@PathVariable UUID id) {
        billPaymentService.cancelRecurring(currentUserProvider.currentUserId(), id);
    }
}
