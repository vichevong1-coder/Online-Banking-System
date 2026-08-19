package com.obs.backend.feature.beneficiary.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = com.obs.backend.feature.beneficiary.controller.BeneficiaryController.class)
public class BeneficiaryExceptionHandler {

    // Also covers "exists but is somebody else's beneficiary" — 404, not 403, so
    // a beneficiary id can't be used to probe whose it is.
    @ExceptionHandler(BeneficiaryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBeneficiaryNotFound(BeneficiaryNotFoundException e) {
        return new ErrorResponse("BENEFICIARY_NOT_FOUND", e.getMessage());
    }

    // 409, matching PHONE_ALREADY_REGISTERED: the request is well-formed, it just
    // conflicts with a row that already exists.
    @ExceptionHandler(DuplicateBeneficiaryException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateBeneficiary(DuplicateBeneficiaryException e) {
        return new ErrorResponse("BENEFICIARY_ALREADY_EXISTS", e.getMessage());
    }

    /**
     * The service pre-checks for a duplicate, so this only fires when two
     * concurrent requests both pass that check and the UNIQUE constraint decides
     * between them. Scoped to this controller ({@code assignableTypes}) so it
     * cannot relabel some unrelated feature's constraint violation as a duplicate
     * beneficiary.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConstraintViolation(DataIntegrityViolationException e) {
        return new ErrorResponse("BENEFICIARY_ALREADY_EXISTS", "This beneficiary is already saved");
    }
}
