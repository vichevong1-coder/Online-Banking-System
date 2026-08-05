package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.dto.RegisterRequest;
import com.obs.backend.feature.auth.dto.RegisterResponse;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.auth.exception.PhoneAlreadyRegisteredException;
import com.obs.backend.feature.auth.mapper.RegistrationMapper;
import com.obs.backend.feature.auth.service.OtpService;
import com.obs.backend.feature.auth.service.RegistrationService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public RegistrationServiceImpl(
            UserRepository userRepository,
            RegistrationMapper registrationMapper,
            PasswordEncoder passwordEncoder,
            OtpService otpService) {
        this.userRepository = userRepository;
        this.registrationMapper = registrationMapper;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedPhone = request.phone().trim();
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new PhoneAlreadyRegisteredException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = registrationMapper.toEntity(request, normalizedPhone, passwordHash);
        User saved = userRepository.save(user);
        otpService.generateAndSend(saved, OtpPurpose.REGISTRATION);
        return registrationMapper.toResponse(saved);
    }
}
