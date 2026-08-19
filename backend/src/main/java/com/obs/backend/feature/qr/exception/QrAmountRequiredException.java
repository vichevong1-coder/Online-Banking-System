package com.obs.backend.feature.qr.exception;

/**
 * The payload carries no amount, so the request had to supply one — and a
 * currency with it. Reported as {@code VALIDATION_FAILED} with the missing
 * fields named, because that is exactly what it is: a field-level omission Bean
 * Validation could not see, since whether the fields are required depends on the
 * payload.
 */
public class QrAmountRequiredException extends RuntimeException {

    public QrAmountRequiredException() {
        super("This QR code does not carry an amount, so one must be supplied.");
    }
}
