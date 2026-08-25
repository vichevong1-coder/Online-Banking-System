package com.obs.backend.feature.statement.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.obs.backend.feature.statement")
public class StatementExceptionHandler {

    @ExceptionHandler(NoProfileEmailException.class)
    public ResponseEntity<ErrorResponse> handleNoProfileEmail(NoProfileEmailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("NO_PROFILE_EMAIL", ex.getMessage()));
    }
}
