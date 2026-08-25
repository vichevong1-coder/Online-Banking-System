package com.obs.backend.feature.notification.service.impl;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.notification.dto.NotificationResponse;
import com.obs.backend.feature.notification.entity.Notification;
import com.obs.backend.feature.notification.entity.NotificationType;
import com.obs.backend.feature.notification.exception.NotificationNotFoundException;
import com.obs.backend.feature.notification.repository.NotificationRepository;
import com.obs.backend.feature.notification.service.NotificationSender;
import com.obs.backend.feature.notification.service.NotificationService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationSender notificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
    }

    @Override
    @Transactional
    public void sendNotification(UUID userId, String title, String message, NotificationType type) {
        Notification notification = new Notification(userId, title, message, type);
        notificationRepository.save(notification);
        notificationSender.send(userId, title, message, type);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        // orElseThrow, not ifPresent: silently succeeding on an id that does not exist (or
        // belongs to somebody else) reported 204 for a no-op. The lookup is already scoped to
        // the caller, so another customer's notification is indistinguishable from a missing
        // one — the Sprint 2 404-not-403 rule rather than a leak.
        notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new)
                .markAsRead();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt());
    }
}
