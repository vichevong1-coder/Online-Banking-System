package com.obs.backend.feature.devtools;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory record of the most recent "SMS" messages sent in dev. Memory only on purpose: the OTP
 * plaintext is never written to the database, and the outbox resets on restart, which is fine for
 * a demo.
 */
@Component
@Profile("dev")
public class DevSmsOutbox {

    static final int MAX_MESSAGES = 50;

    public record DevSms(String phone, String message, Instant sentAt) {}

    private final Deque<DevSms> messages = new ArrayDeque<>();

    public synchronized void add(String phone, String message) {
        messages.addFirst(new DevSms(phone, message, Instant.now()));
        while (messages.size() > MAX_MESSAGES) {
            messages.removeLast();
        }
    }

    /** Newest first. */
    public synchronized List<DevSms> recent() {
        return List.copyOf(messages);
    }
}
