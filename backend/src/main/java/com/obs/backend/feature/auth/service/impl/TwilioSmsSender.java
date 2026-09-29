package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.service.OtpSender;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class TwilioSmsSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);

    @Value("${twilio.account-sid:placeholder}")
    private String accountSid;

    @Value("${twilio.auth-token:placeholder}")
    private String authToken;

    @Value("${twilio.from-number:+1234567890}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!"placeholder".equals(accountSid)) {
            Twilio.init(accountSid, authToken);
        }
    }

    @Override
    public void send(String phoneNumber, String code) {
        if ("placeholder".equals(accountSid)) {
            log.info("Twilio is not configured. Fake sending to {}: {}", phoneNumber, code);
            return;
        }
        try {
            Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                "Your OBS OTP code is: " + code
            ).create();
            log.info("Sent SMS via Twilio to {}", phoneNumber);
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}", phoneNumber, e);
        }
    }
}
