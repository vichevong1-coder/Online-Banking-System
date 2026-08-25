package com.obs.backend.feature.user.dto;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
        @Email(message = "Enter a valid email address")
        String email
) {}
