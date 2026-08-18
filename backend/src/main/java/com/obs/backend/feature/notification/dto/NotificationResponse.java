package com.obs.backend.feature.notification.dto;

import com.obs.backend.feature.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt) {}
