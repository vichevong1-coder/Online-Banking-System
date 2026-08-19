package com.obs.backend.feature.qr.service;

/** The second field of a payload: what its target names. */
public enum QrTargetType {
    /** Personal — the target is a 12-digit account number (US-032/US-033). */
    P,
    /** Merchant — the target is a merchant code (US-034). */
    M
}
