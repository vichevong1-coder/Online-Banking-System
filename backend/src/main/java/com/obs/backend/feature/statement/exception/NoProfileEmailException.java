package com.obs.backend.feature.statement.exception;

public class NoProfileEmailException extends RuntimeException {
    public NoProfileEmailException() {
        super("Customer has no email address configured");
    }
}
