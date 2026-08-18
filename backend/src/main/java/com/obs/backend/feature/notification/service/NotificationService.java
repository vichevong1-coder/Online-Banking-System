package com.obs.backend.feature.notification.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.notification.dto.NotificationResponse;
import com.obs.backend.feature.notification.entity.NotificationType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void sendNotification(UUID userId, String title, String message, NotificationType type);

    PageResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID userId, UUID notificationId);

    long getUnreadCount(UUID userId);
}
