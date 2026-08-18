package com.obs.backend.feature.admin.dto;

import com.obs.backend.security.AccountStatus;
import jakarta.validation.constraints.NotNull;

// An unknown status string fails Jackson's enum binding and comes back as a 400 from
// GlobalExceptionHandler; @NotNull covers the absent/explicit-null case.
public record UpdateCustomerStatusRequest(@NotNull AccountStatus status) {
}
