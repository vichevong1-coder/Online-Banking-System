package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyRequest(@NotBlank String challengeToken, @NotBlank String code) {
}
