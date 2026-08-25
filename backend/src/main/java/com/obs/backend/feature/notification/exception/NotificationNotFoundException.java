package com.obs.backend.feature.notification.exception;

/**
 * Also thrown when the notification exists but belongs to another customer —
 * 404 rather than 403, the Sprint 2 rule, so ids cannot probe ownership.
 */
public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
