package com.obs.backend.feature.bill.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {"com.obs.backend.feature.bill", "com.obs.backend.feature.admin.controller"})
public class BillExceptionHandler {

    @ExceptionHandler(BillProviderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotFound(BillProviderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("BILL_PROVIDER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidBillAccountNumberException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccount(InvalidBillAccountNumberException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_BILL_ACCOUNT_NUMBER", ex.getMessage()));
    }

    @ExceptionHandler(BillPaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(BillPaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("BILL_PAYMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RecurringBillNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRecurringNotFound(RecurringBillNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RECURRING_BILL_NOT_FOUND", ex.getMessage()));
    }
}
