package com.obs.backend.feature.auth;

import com.obs.backend.feature.auth.service.OtpSender;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * OTP codes are stored hashed, so tests can't recover them from the database. This bean captures
 * the last code sent per phone number, standing in for the real SMS provider (@Primary overrides
 * the log-only LoggingSmsSender regardless of active profile).
 */
@TestConfiguration
public class RecordingOtpSenderConfig {

    // Concrete return type (not OtpSender) so tests can @Autowired RecordingOtpSender directly
    // and read back the code that was "sent", in addition to it satisfying OtpSender injection
    // sites in application code.
    @Bean
    @Primary
    public RecordingOtpSender otpSender() {
        return new RecordingOtpSender();
    }

    public static class RecordingOtpSender implements OtpSender {
        private final ConcurrentMap<String, String> lastCodeByPhone = new ConcurrentHashMap<>();

        @Override
        public void send(String phoneNumber, String code) {
            lastCodeByPhone.put(phoneNumber, code);
        }

        public String lastCodeFor(String phoneNumber) {
            return lastCodeByPhone.get(phoneNumber);
        }
    }
}
