package com.obs.backend.feature.auth.service;

import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.user.entity.User;

public interface OtpService {

    void generateAndSend(User user, OtpPurpose purpose);

    void verify(User user, OtpPurpose purpose, String code);

    /** Looks up the user by phone, verifies the REGISTRATION code, and marks the phone verified. */
    void verifyRegistration(String phone, String code);

    /** Looks up the user by phone and sends a fresh REGISTRATION code. */
    void resendRegistration(String phone);
}
