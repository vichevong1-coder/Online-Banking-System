package com.obs.backend.security;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    LOCKED,
    /** Registered and phone-verified, waiting for staff to review the NID photos and selfie. */
    PENDING_APPROVAL,
    /** Staff rejected the KYC submission. Terminal: the customer registers again. */
    REJECTED
}
