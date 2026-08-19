package com.obs.backend.feature.qr.exception;

import com.obs.backend.common.dto.ValidationErrorResponse;
import com.obs.backend.feature.account.dto.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Every rejection on this slice is a 400 with a coded body, matching the
 * transfer slice. In particular a declined merchant is a 400 and not a 402: every
 * business rejection next door — over-limit, insufficient funds, currency
 * mismatch — is already a 400, and one endpoint inventing a different status for
 * the same class of outcome would be worse than a slightly loose status name.
 */
@RestControllerAdvice
public class QrExceptionHandler {

    @ExceptionHandler(QrPayloadInvalidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePayloadInvalid(QrPayloadInvalidException e) {
        return new ErrorResponse("QR_PAYLOAD_INVALID", e.getMessage());
    }

    // 400, not 404: the request is well-formed, it is the scanned code that names
    // something that does not exist.
    @ExceptionHandler(QrTargetNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTargetNotFound(QrTargetNotFoundException e) {
        return new ErrorResponse("QR_TARGET_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(QrAmountMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAmountMismatch(QrAmountMismatchException e) {
        return new ErrorResponse("QR_AMOUNT_MISMATCH", e.getMessage());
    }

    /**
     * Deliberately the same {@code VALIDATION_FAILED} shape @Valid failures come
     * back as: the fields really are missing, and a caller should not have to
     * handle two different bodies for "you left the amount out" depending on
     * whether the rule needed the payload to be seen.
     */
    @ExceptionHandler(QrAmountRequiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleAmountRequired(QrAmountRequiredException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("amount", e.getMessage());
        fieldErrors.put("currency", e.getMessage());
        return new ValidationErrorResponse("VALIDATION_FAILED", "One or more fields are invalid", fieldErrors);
    }

    @ExceptionHandler(MerchantDeclinedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMerchantDeclined(MerchantDeclinedException e) {
        return new ErrorResponse("MERCHANT_DECLINED", e.getMessage());
    }
}
