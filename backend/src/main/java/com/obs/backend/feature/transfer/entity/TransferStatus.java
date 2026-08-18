package com.obs.backend.feature.transfer.entity;

public enum TransferStatus {
    /** Both ledger legs were written and balances are final. */
    COMPLETED,
    /**
     * Accepted and debited, awaiting settlement at the receiving bank. Only
     * interbank transfers (US-026) reach this state; the demo settles them on a
     * simulated response rather than a real clearing cycle.
     */
    PENDING,
    /** Rejected before any balance moved, or reversed after a failed settlement. */
    FAILED
}
