package com.obs.backend.feature.admin.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
