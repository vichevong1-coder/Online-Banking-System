package com.obs.backend.feature.auth.service;

import com.obs.backend.feature.auth.dto.RegisterRequest;
import com.obs.backend.feature.auth.dto.RegisterResponse;

public interface RegistrationService {

    RegisterResponse register(RegisterRequest request);
}
