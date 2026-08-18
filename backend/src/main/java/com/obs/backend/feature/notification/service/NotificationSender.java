package com.obs.backend.feature.notification.service;

import com.obs.backend.feature.notification.entity.NotificationType;
import java.util.UUID;

public interface NotificationSender {

    void send(UUID userId, String title, String message, NotificationType type);
}
