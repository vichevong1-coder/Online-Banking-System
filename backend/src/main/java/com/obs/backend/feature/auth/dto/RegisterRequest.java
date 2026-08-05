package com.obs.backend.feature.auth.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String nidNumber,
        @NotNull @Future LocalDate nidExpiryDate,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank String phone) {
}
