package com.obs.backend.feature.auth.exception;

import com.obs.backend.feature.auth.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(PhoneAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicatePhone(PhoneAlreadyRegisteredException e) {
        return new ErrorResponse("PHONE_ALREADY_REGISTERED", e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException e) {
        return new ErrorResponse("INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(InvalidLoginIdentifierException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidLoginIdentifier(InvalidLoginIdentifierException e) {
        return new ErrorResponse("IDENTIFIER_REQUIRED", e.getMessage());
    }

    @ExceptionHandler(PhoneNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handlePhoneNotVerified(PhoneNotVerifiedException e) {
        return new ErrorResponse("PHONE_NOT_VERIFIED", e.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidOtp(InvalidOtpException e) {
        return new ErrorResponse("INVALID_OR_EXPIRED_OTP", e.getMessage());
    }

    @ExceptionHandler(InvalidChallengeTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidChallengeToken(InvalidChallengeTokenException e) {
        return new ErrorResponse("INVALID_OR_EXPIRED_CHALLENGE", e.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return new ErrorResponse("INVALID_OR_EXPIRED_REFRESH_TOKEN", e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFound(UserNotFoundException e) {
        return new ErrorResponse("USER_NOT_FOUND", e.getMessage());
    }

    // Thrown by AccountStatusPolicy — shared by every login path (US-006).
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleSuspendedAccount(DisabledException e) {
        return new ErrorResponse("ACCOUNT_SUSPENDED", e.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleLockedAccount(LockedException e) {
        return new ErrorResponse("ACCOUNT_LOCKED", e.getMessage());
    }
}
