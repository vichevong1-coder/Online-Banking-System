package com.obs.backend.feature.notification.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Scoped to the notification controller (assignableTypes), matching
// BeneficiaryExceptionHandler: an advice this narrow cannot relabel some other
// feature's not-found as a missing notification.
@RestControllerAdvice(assignableTypes = com.obs.backend.feature.notification.controller.NotificationController.class)
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotificationNotFound(NotificationNotFoundException e) {
        return new ErrorResponse("NOTIFICATION_NOT_FOUND", e.getMessage());
    }
}
