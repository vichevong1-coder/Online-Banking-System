package com.obs.backend.feature.transfer.exception;

/**
 * US-027: source and destination hold different currencies. There is no rate
 * table and no conversion service — multi-currency is a display feature, so the
 * transfer is rejected rather than converted.
 */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException() {
        super("Source and destination accounts must hold the same currency");
    }
}
