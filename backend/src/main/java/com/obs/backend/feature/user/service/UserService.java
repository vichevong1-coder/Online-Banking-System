package com.obs.backend.feature.user.service;

import com.obs.backend.feature.user.dto.ChangePasswordRequest;
import com.obs.backend.feature.user.dto.UpdateProfileRequest;
import com.obs.backend.feature.user.dto.UserProfileResponse;
import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
    void changePassword(UUID userId, ChangePasswordRequest request);
}
