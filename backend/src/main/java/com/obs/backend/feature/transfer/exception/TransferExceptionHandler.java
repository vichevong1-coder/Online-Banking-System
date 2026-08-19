package com.obs.backend.feature.transfer.exception;

import com.obs.backend.feature.account.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransferExceptionHandler {

    // Also covers "exists but is somebody else's transfer" — 404, not 403, so a
    // transfer id can't be used to probe whose it is.
    @ExceptionHandler(TransferNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTransferNotFound(TransferNotFoundException e) {
        return new ErrorResponse("TRANSFER_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCurrencyMismatch(CurrencyMismatchException e) {
        return new ErrorResponse("CURRENCY_MISMATCH", e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException e) {
        return new ErrorResponse("INSUFFICIENT_FUNDS", e.getMessage());
    }

    @ExceptionHandler(SameAccountTransferException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleSameAccountTransfer(SameAccountTransferException e) {
        return new ErrorResponse("SAME_ACCOUNT_TRANSFER", e.getMessage());
    }

    // Carries its own code so the caller can tell a single oversized transfer
    // (TRANSFER_LIMIT_EXCEEDED) from a day's worth of them (DAILY_LIMIT_EXCEEDED).
    @ExceptionHandler(TransferLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLimitExceeded(TransferLimitExceededException e) {
        return new ErrorResponse(e.getErrorCode(), e.getMessage());
    }

    // The @Version column on accounts (V9) turns a lost update into this. Retrying
    // is the caller's call, not ours: replaying a debit blindly could double-spend.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConcurrentModification(ObjectOptimisticLockingFailureException e) {
        return new ErrorResponse(
                "CONCURRENT_MODIFICATION", "An account involved in this transfer changed concurrently. Try again.");
    }
}
