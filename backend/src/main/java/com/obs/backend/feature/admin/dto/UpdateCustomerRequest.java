package com.obs.backend.feature.admin.dto;

import com.obs.backend.feature.user.entity.User.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record UpdateCustomerRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone format")
        String phone,

        String nidNumber,
        LocalDate nidExpiryDate,
        LocalDate dateOfBirth,
        Gender gender
) {}
