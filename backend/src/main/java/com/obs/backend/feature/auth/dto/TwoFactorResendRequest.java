package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorResendRequest(@NotBlank String challengeToken) {
}
