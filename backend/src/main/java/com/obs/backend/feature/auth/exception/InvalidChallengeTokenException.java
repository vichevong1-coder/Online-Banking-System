package com.obs.backend.feature.auth.exception;

public class InvalidChallengeTokenException extends RuntimeException {

    public InvalidChallengeTokenException() {
        super("Invalid or expired challenge token");
    }
}
