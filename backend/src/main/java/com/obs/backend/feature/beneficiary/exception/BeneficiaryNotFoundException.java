package com.obs.backend.feature.beneficiary.exception;

/**
 * Also thrown when the beneficiary exists but belongs to another customer —
 * 404 rather than 403, the Sprint 2 rule, so ids cannot probe ownership.
 */
public class BeneficiaryNotFoundException extends RuntimeException {
    public BeneficiaryNotFoundException() {
        super("Beneficiary not found");
    }
}
