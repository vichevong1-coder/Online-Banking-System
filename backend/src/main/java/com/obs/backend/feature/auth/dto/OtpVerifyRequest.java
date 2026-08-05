package com.obs.backend.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(@NotBlank String phone, @NotBlank String code) {
}
