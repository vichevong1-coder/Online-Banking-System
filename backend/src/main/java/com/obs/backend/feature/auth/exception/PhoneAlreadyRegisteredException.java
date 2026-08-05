package com.obs.backend.feature.auth.exception;

public class PhoneAlreadyRegisteredException extends RuntimeException {

    public PhoneAlreadyRegisteredException() {
        super("Phone number already registered");
    }
}
