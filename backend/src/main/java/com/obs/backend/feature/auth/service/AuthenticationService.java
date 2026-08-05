package com.obs.backend.feature.auth.service;

import com.obs.backend.feature.auth.dto.AuthTokenResponse;
import com.obs.backend.feature.auth.dto.LoginRequest;
import com.obs.backend.feature.auth.dto.LoginResponse;
import com.obs.backend.feature.auth.dto.RefreshRequest;
import com.obs.backend.feature.auth.dto.RefreshResponse;
import com.obs.backend.feature.auth.dto.TwoFactorResendRequest;
import com.obs.backend.feature.auth.dto.TwoFactorVerifyRequest;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);

    AuthTokenResponse verifyTwoFactor(TwoFactorVerifyRequest request);

    /** Issues a fresh OTP and a fresh challenge token for a login still in progress. */
    LoginResponse resendTwoFactor(TwoFactorResendRequest request);

    RefreshResponse refresh(RefreshRequest request);
}
