package com.obs.backend.feature.qr.exception;

/**
 * A well-formed payload naming an account number or merchant code that does not
 * exist. A 400 rather than a 404: the request itself is fine, it is the scanned
 * code that is stale or fake.
 */
public class QrTargetNotFoundException extends RuntimeException {

    public QrTargetNotFoundException() {
        super("This QR code does not name an account or merchant we know.");
    }
}
