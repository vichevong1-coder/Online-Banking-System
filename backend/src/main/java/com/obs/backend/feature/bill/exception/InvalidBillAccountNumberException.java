package com.obs.backend.feature.bill.exception;

public class InvalidBillAccountNumberException extends RuntimeException {
    public InvalidBillAccountNumberException() {
        super("Bill account number does not match the provider's required format");
    }
}
