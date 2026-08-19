package com.obs.backend.feature.beneficiary.exception;

/**
 * The caller already has this bank code + account number saved. Scoped to the
 * caller: two customers saving the same payee is not a duplicate.
 */
public class DuplicateBeneficiaryException extends RuntimeException {
    public DuplicateBeneficiaryException() {
        super("This beneficiary is already saved");
    }
}
