package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

// Customer keys by phone; admin/staff keys by email (US-010) — exactly one of the two must be
// present, enforced in AuthenticationService rather than with bean validation.
public record LoginRequest(String email, String phone, @NotBlank String password) {
}
