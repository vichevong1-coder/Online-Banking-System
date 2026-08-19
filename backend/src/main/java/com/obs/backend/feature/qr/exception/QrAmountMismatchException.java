package com.obs.backend.feature.qr.exception;

/**
 * The payload fixes an amount and the request asked to pay a different one.
 * This is what stops a merchant's displayed price being quietly underpaid.
 */
public class QrAmountMismatchException extends RuntimeException {

    public QrAmountMismatchException() {
        super("This QR code is for a different amount.");
    }
}
