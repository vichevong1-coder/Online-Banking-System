package com.obs.backend.feature.user.service.impl;

import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.user.dto.ChangePasswordRequest;
import com.obs.backend.feature.user.dto.UpdateProfileRequest;
import com.obs.backend.feature.user.dto.UserProfileResponse;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.exception.InvalidCurrentPasswordException;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.feature.user.service.UserService;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.obs.backend.security.jwt.RevokedTokenRepository revokedTokenRepository;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, com.obs.backend.security.jwt.RevokedTokenRepository revokedTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (request.email() != null) {
            user.setEmail(request.email().isBlank() ? null : request.email().trim());
        }
        user = userRepository.save(user);
        return toResponse(user);
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
        
        com.obs.backend.security.jwt.RevokedToken rt = new com.obs.backend.security.jwt.RevokedToken("USER-" + userId);
        revokedTokenRepository.save(rt);
    }

    private UserProfileResponse toResponse(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhone(),
                u.getNidNumber(),
                u.getNidExpiryDate(),
                u.getDateOfBirth(),
                u.getGender(),
                u.getEmail(),
                u.getRole(),
                u.getStatus(),
                u.getCreatedAt()
        );
    }
}
