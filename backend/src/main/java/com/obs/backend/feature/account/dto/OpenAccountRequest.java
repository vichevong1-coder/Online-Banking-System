package com.obs.backend.feature.account.dto;

import com.obs.backend.feature.account.entity.AccountType;
import com.obs.backend.feature.account.entity.Currency;
import jakarta.validation.constraints.NotNull;

// US-014: auto-approved on submit — there is no admin review step for this in Sprint 2, so the
// account is created ACTIVE with a zero balance immediately.
public record OpenAccountRequest(@NotNull AccountType accountType, @NotNull Currency currency) {
}
