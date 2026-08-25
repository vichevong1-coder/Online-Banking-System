package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Identifier is required")
        String identifier
) {}
