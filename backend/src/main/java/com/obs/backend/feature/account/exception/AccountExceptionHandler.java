package com.obs.backend.feature.account.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

    // Also covers "exists but belongs to another user" — 404 rather than 403 so account
    // ownership can't be probed by ID.
    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFound(AccountNotFoundException e) {
        return new ErrorResponse("ACCOUNT_NOT_FOUND", e.getMessage());
    }
}
