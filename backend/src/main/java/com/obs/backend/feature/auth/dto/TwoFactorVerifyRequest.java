package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyRequest(
        @NotBlank String challengeToken,
        // @NotNull rather than @NotBlank: the pattern already rejects "", and two violations on
        // one field would give a nondeterministic message (GlobalExceptionHandler keeps one).
        @NotNull @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits") String code) {
}
