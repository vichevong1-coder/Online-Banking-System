package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Identifier is required")
        String identifier,

        // @NotNull rather than @NotBlank: the pattern already rejects "", and two violations on
        // one field would give a nondeterministic message (GlobalExceptionHandler keeps one).
        @NotNull(message = "Verification code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits")
        String code,

        // Capped at 100 like RegisterRequest: bcrypt silently ignores input past 72 bytes, so an
        // unbounded password buys nothing.
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword
) {}
