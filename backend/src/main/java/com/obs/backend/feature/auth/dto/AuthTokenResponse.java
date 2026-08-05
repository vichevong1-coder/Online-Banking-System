package com.obs.backend.feature.auth.dto;

import com.obs.backend.security.Role;
import java.util.UUID;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UUID userId,
        String firstName,
        String lastName,
        Role role) {
}
