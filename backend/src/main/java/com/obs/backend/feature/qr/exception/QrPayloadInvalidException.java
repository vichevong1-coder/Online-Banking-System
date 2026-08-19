package com.obs.backend.feature.qr.exception;

/** A scanned string that is not a payload at all: wrong marker, wrong shape, unknown type. */
public class QrPayloadInvalidException extends RuntimeException {

    public QrPayloadInvalidException() {
        super("This QR code could not be read.");
    }
}
