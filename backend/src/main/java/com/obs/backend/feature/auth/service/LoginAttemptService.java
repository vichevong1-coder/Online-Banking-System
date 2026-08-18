package com.obs.backend.feature.auth.service;

public interface LoginAttemptService {

    void recordAttempt(String identifier, boolean success, String ipAddress, String userAgent);
}
