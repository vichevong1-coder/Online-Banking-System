package com.obs.backend.feature.notification.service.impl;

import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.service.NotificationSender;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(UUID userId, String title, String message, NotificationType type) {
        log.info("Push notification sent to user {}: [{}] {} - {}", userId, type, title, message);
    }
}
