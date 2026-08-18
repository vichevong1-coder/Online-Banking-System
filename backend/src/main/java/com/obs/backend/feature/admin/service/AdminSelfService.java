package com.obs.backend.feature.admin.service;

import com.obs.backend.feature.admin.dto.ChangePasswordRequest;
import java.util.UUID;

public interface AdminSelfService {

    void changePassword(UUID userId, ChangePasswordRequest request);
}
