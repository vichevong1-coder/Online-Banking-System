package com.obs.backend.feature.admin.dto;

import com.obs.backend.security.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffRoleRequest(
        @NotNull(message = "Role is required")
        Role role) {}
