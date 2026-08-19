package com.obs.backend.feature.qr.exception;

/**
 * US-034: the merchant refused the payment. Raised after the FAILED transfer row
 * is written and before any balance is touched, so the decline is visible in
 * history without any money having moved.
 */
public class MerchantDeclinedException extends RuntimeException {

    public MerchantDeclinedException(String displayName) {
        super("%s declined this payment.".formatted(displayName));
    }
}
