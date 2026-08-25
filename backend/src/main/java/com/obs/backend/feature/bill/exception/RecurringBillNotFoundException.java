package com.obs.backend.feature.bill.exception;

public class RecurringBillNotFoundException extends RuntimeException {
    public RecurringBillNotFoundException() {
        super("Recurring bill payment not found");
    }
}
