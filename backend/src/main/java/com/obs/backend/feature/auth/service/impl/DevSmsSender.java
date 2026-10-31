package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.service.OtpSender;
import com.obs.backend.feature.devtools.DevSmsOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// US-060 (Sprint 6) swaps this for a live SMS provider behind the same OtpSender interface.
// No real provider is wired up for local dev. Each "SMS" is made visible three ways so a demo
// never depends on the backend log: the log line, the /dev/sms page, and a copy in Mailpit.
@Component
@Profile("dev")
public class DevSmsSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(DevSmsSender.class);
    private static final String FROM = "sms-gateway@obs.local";

    private final DevSmsOutbox outbox;
    private final JavaMailSender mailSender;

    public DevSmsSender(DevSmsOutbox outbox, JavaMailSender mailSender) {
        this.outbox = outbox;
        this.mailSender = mailSender;
    }

    @Override
    public void send(String phoneNumber, String code) {
        String message = "Your OBS verification code is: " + code + ". It expires in 5 minutes.";
        log.info("OTP for {}: {}", phoneNumber, code);
        outbox.add(phoneNumber, message);
        mirrorToMailpit(phoneNumber, message);
    }

    // Best effort: a missing Mailpit must never break login or signup.
    private void mirrorToMailpit(String phoneNumber, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(FROM);
            mail.setTo(phoneNumber.replaceAll("[^0-9+]", "") + "@sms.obs.local");
            mail.setSubject("SMS to " + phoneNumber);
            mail.setText(message);
            mailSender.send(mail);
        } catch (Exception e) {
            log.warn("Could not mirror SMS to Mailpit: {}", e.getMessage());
        }
    }
}
