package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Customer keys by phone; admin/staff keys by email (US-010) — exactly one of the two must be
// present, enforced in AuthenticationService rather than with bean validation.
// Both are capped at 100: whichever one is supplied is handed to LoginAttemptService, which
// writes it to login_attempts.identifier VARCHAR(100) on the failure path.
public record LoginRequest(
        @Size(max = 100) String email, @Size(max = 100) String phone, @NotBlank String password) {
}
