package com.obs.backend.feature.bill.exception;

public class BillPaymentNotFoundException extends RuntimeException {
    public BillPaymentNotFoundException() {
        super("Bill payment not found");
    }
}
