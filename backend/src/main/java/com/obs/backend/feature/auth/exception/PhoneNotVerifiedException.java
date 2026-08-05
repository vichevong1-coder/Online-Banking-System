package com.obs.backend.feature.auth.exception;

public class PhoneNotVerifiedException extends RuntimeException {

    public PhoneNotVerifiedException() {
        super("Phone number is not verified");
    }
}
