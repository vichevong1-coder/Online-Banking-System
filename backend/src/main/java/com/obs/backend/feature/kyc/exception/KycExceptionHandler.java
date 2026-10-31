package com.obs.backend.feature.kyc.exception;

import com.obs.backend.feature.auth.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class KycExceptionHandler {

    @ExceptionHandler(KycException.class)
    public ResponseEntity<ErrorResponse> handleKyc(KycException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorResponse(e.getCode(), e.getMessage()));
    }
}
