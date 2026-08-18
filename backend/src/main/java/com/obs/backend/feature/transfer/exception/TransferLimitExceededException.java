package com.obs.backend.feature.transfer.exception;

import java.math.BigDecimal;

/**
 * US-027 caps are a hard block, not a flag: nothing is queued for review, the
 * request is rejected with a 400. US-040 (fraud/risk review) is cut from the
 * plan, so there is nowhere for a flagged transfer to go.
 */
public class TransferLimitExceededException extends RuntimeException {

    private final String errorCode;

    private TransferLimitExceededException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static TransferLimitExceededException perTransfer(BigDecimal limit) {
        return new TransferLimitExceededException(
                "TRANSFER_LIMIT_EXCEEDED", "This transfer is above the per-transfer limit of " + limit.toPlainString());
    }

    public static TransferLimitExceededException daily(BigDecimal limit) {
        return new TransferLimitExceededException(
                "DAILY_LIMIT_EXCEEDED", "This transfer would exceed your daily transfer limit of " + limit.toPlainString());
    }

    public String getErrorCode() {
        return errorCode;
    }
}
