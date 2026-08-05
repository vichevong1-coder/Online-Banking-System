package com.obs.backend.feature.auth.service;

public interface OtpSender {
    void send(String phoneNumber, String code);
}
