package com.obs.backend.feature.auth.exception;

public class InvalidLoginIdentifierException extends RuntimeException {

    public InvalidLoginIdentifierException() {
        super("Provide either email or phone, not both or neither");
    }
}
