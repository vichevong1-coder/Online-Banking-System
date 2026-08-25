package com.obs.backend.feature.bill.exception;

public class BillProviderNotFoundException extends RuntimeException {
    public BillProviderNotFoundException() {
        super("Bill provider not found");
    }
}
