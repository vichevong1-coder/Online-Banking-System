package com.obs.backend.feature.auth.dto;

import java.util.UUID;

public record RegisterResponse(UUID id, String firstName, String lastName, String phone) {
}
