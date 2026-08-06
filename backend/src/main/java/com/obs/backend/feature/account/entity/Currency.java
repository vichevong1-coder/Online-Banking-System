package com.obs.backend.feature.account.entity;

// US-016: each account holds a single currency; a customer goes multi-currency by holding
// several accounts. Scoped to what the KYC flow (US-007) actually supports today.
public enum Currency {
    USD,
    KHR
}
