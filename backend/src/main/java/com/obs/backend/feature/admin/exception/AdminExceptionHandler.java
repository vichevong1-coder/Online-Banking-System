package com.obs.backend.feature.admin.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminExceptionHandler {

    // Also covers "the id exists but belongs to a staff/admin row" — the admin customer screens
    // address customers only, and a staff id must not resolve through a customer endpoint.
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCustomerNotFound(CustomerNotFoundException e) {
        return new ErrorResponse("CUSTOMER_NOT_FOUND", e.getMessage());
    }
}
