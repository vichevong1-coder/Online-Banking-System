package com.obs.backend.feature.admin.dto;

import com.obs.backend.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 255)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 255)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255)
        String email,

        // Bounded to the phone VARCHAR(30) column so oversized input is a 400, not a 500.
        // @NotNull rather than @NotBlank: the pattern already rejects "", and two violations on
        // one field would give a nondeterministic message (GlobalExceptionHandler keeps one).
        @NotNull(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,28}$", message = "Enter a valid phone number")
        String phone,

        @NotNull(message = "Role is required")
        Role role,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password) {}
