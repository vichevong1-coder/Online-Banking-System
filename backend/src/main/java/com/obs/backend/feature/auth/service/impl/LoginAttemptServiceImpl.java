package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.entity.LoginAttempt;
import com.obs.backend.feature.auth.repository.LoginAttemptRepository;
import com.obs.backend.feature.auth.service.LoginAttemptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptServiceImpl(LoginAttemptRepository loginAttemptRepository) {
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(String identifier, boolean success, String ipAddress, String userAgent) {
        String safeIdentifier = identifier != null ? identifier : "unknown";
        LoginAttempt attempt = new LoginAttempt(safeIdentifier, success, ipAddress, userAgent);
        loginAttemptRepository.saveAndFlush(attempt);
    }
}
