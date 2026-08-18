package com.obs.backend.feature.admin.service.impl;

import com.obs.backend.feature.admin.dto.ChangePasswordRequest;
import com.obs.backend.feature.admin.exception.InvalidCurrentPasswordException;
import com.obs.backend.feature.admin.service.AdminSelfService;
import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSelfServiceImpl implements AdminSelfService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSelfServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
