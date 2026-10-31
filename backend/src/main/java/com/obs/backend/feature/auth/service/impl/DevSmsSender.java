package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.service.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// US-060 (Sprint 6) swaps this for a live SMS provider behind the same OtpSender interface.
// No real provider wired up for local dev — the code goes to the backend log only.
@Component
@Profile("dev")
public class LoggingSmsSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phoneNumber, String code) {
        log.info("OTP for {}: {}", phoneNumber, code);
    }
}
