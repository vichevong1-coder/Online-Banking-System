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

    @ExceptionHandler(StaffNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleStaffNotFound(StaffNotFoundException e) {
        return new ErrorResponse("STAFF_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCurrentPassword(InvalidCurrentPasswordException e) {
        return new ErrorResponse("INVALID_CURRENT_PASSWORD", e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyRegistered(EmailAlreadyRegisteredException e) {
        return new ErrorResponse("EMAIL_ALREADY_REGISTERED", e.getMessage());
    }

    @ExceptionHandler(com.obs.backend.feature.auth.exception.PhoneAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handlePhoneAlreadyRegistered(com.obs.backend.feature.auth.exception.PhoneAlreadyRegisteredException e) {
        return new ErrorResponse("PHONE_ALREADY_REGISTERED", e.getMessage());
    }
}
