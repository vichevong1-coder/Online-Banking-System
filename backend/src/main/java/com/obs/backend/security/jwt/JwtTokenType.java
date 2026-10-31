package com.obs.backend.security.jwt;

public enum JwtTokenType {
    ACCESS,
    REFRESH,
    CHALLENGE,
    /** Issued on registration OTP verification; only authorises uploading that user's KYC images. */
    KYC_UPLOAD
}
