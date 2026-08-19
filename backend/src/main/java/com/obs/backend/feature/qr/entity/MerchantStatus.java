package com.obs.backend.feature.qr.entity;

public enum MerchantStatus {
    /** Accepts payments normally. */
    ACTIVE,
    /**
     * Refuses every payment (US-034). The demo's failure path, expressed as data
     * rather than a hardcoded merchant code in the service — a payment to one of
     * these writes a FAILED transfer, moves no money and comes back as
     * {@code 400 MERCHANT_DECLINED}.
     */
    ALWAYS_DECLINES
}
