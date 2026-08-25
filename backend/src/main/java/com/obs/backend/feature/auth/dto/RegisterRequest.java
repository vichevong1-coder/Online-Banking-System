package com.obs.backend.feature.auth.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Size(min = 8, max = 100) String password,
        // Cambodian NID numbers are exactly 9 digits. @NotNull rather than @NotBlank so an
        // empty string produces one message (the @Pattern one) instead of two competing ones —
        // GlobalExceptionHandler keeps only the last error per field.
        @NotNull @Pattern(regexp = "\\d{9}", message = "must be exactly 9 digits") String nidNumber,
        @NotNull @Future LocalDate nidExpiryDate,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        // Bounded to the phone VARCHAR(30) column so oversized input is a 400, not a 500.
        // @NotNull rather than @NotBlank: the pattern already rejects "" (see nidNumber above).
        @NotNull @Pattern(regexp = "^\\+?[0-9][0-9 ()-]{6,28}$", message = "must be a valid phone number")
                String phone) {
}
