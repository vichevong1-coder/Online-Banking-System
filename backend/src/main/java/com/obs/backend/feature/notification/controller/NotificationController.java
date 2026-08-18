package com.obs.backend.feature.notification.controller;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.notification.dto.NotificationResponse;
import com.obs.backend.feature.notification.service.NotificationService;
import com.obs.backend.security.CurrentUserProvider;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserProvider currentUserProvider) {
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public PageResponse<NotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationService.getUserNotifications(currentUserProvider.currentUserId(), pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        long count = notificationService.getUnreadCount(currentUserProvider.currentUserId());
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(currentUserProvider.currentUserId(), id);
    }
}
