package com.obs.backend.feature.auth.controller;

import com.obs.backend.feature.auth.dto.AuthTokenResponse;
import com.obs.backend.feature.auth.dto.LoginRequest;
import com.obs.backend.feature.auth.dto.LoginResponse;
import com.obs.backend.feature.auth.dto.RefreshRequest;
import com.obs.backend.feature.auth.dto.RefreshResponse;
import com.obs.backend.feature.auth.dto.TwoFactorResendRequest;
import com.obs.backend.feature.auth.dto.TwoFactorVerifyRequest;
import com.obs.backend.feature.auth.service.AuthenticationService;
import com.obs.backend.feature.audit.AuditService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obs.backend.feature.auth.dto.ForgotPasswordRequest;
import com.obs.backend.feature.auth.dto.ResetPasswordRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AuditService auditService;

    public AuthenticationController(AuthenticationService authenticationService, AuditService auditService) {
        this.authenticationService = authenticationService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        String identifier = request.email() != null ? request.email() : request.phone();
        auditService.logAction(null, identifier, "USER_LOGIN", null, null, "User logged in with identifier: " + identifier);
        return response;
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

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request);
        String identifier = request.identifier();
        auditService.logAction(null, identifier, "PASSWORD_FORGOT", null, null, "Forgot password requested for: " + identifier);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        auditService.logAction(null, null, "PASSWORD_RESET", null, null, "Password reset using token");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody com.obs.backend.feature.auth.dto.LogoutRequest request) {
        authenticationService.logout(request);
        auditService.logAction(null, null, "USER_LOGOUT", null, null, "User logged out");
    }
}
