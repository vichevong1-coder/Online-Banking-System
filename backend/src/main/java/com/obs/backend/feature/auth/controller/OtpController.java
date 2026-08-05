package com.obs.backend.feature.auth.controller;

import com.obs.backend.feature.auth.dto.OtpResendRequest;
import com.obs.backend.feature.auth.dto.OtpVerifyRequest;
import com.obs.backend.feature.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody OtpVerifyRequest request) {
        otpService.verifyRegistration(request.phone().trim(), request.code());
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@Valid @RequestBody OtpResendRequest request) {
        otpService.resendRegistration(request.phone().trim());
    }
}
