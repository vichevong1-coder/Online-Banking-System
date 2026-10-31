package com.obs.backend.feature.auth.service;

import com.obs.backend.feature.auth.dto.OtpVerifyResponse;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.user.entity.User;

public interface OtpService {

    void generateAndSend(User user, OtpPurpose purpose);

    void verify(User user, OtpPurpose purpose, String code);

    /**
     * Looks up the user by phone, verifies the REGISTRATION code, and marks the phone verified.
     * A customer still awaiting KYC approval gets a short-lived upload token for their NID and
     * selfie images; anyone else gets a null token.
     */
    OtpVerifyResponse verifyRegistration(String phone, String code);

    /** Looks up the user by phone and sends a fresh REGISTRATION code. */
    void resendRegistration(String phone);
}
