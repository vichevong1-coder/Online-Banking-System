package com.obs.backend.feature.auth.controller;

import com.obs.backend.feature.auth.dto.AuthTokenResponse;
import com.obs.backend.feature.auth.dto.LoginRequest;
import com.obs.backend.feature.auth.dto.LoginResponse;
import com.obs.backend.feature.auth.dto.RefreshRequest;
import com.obs.backend.feature.auth.dto.RefreshResponse;
import com.obs.backend.feature.auth.dto.TwoFactorResendRequest;
import com.obs.backend.feature.auth.dto.TwoFactorVerifyRequest;
import com.obs.backend.feature.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/2fa/verify")
    public AuthTokenResponse verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return authenticationService.verifyTwoFactor(request);
    }

    @PostMapping("/2fa/resend")
    public LoginResponse resendTwoFactor(@Valid @RequestBody TwoFactorResendRequest request) {
        return authenticationService.resendTwoFactor(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authenticationService.refresh(request);
    }
}
