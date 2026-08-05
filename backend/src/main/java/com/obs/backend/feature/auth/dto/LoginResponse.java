package com.obs.backend.feature.auth.dto;

// 2FA is mandatory for every account (US-011, US-012) — login never returns real tokens
// directly. The challenge token is short-lived and only redeemable at /auth/2fa/verify.
public record LoginResponse(String challengeToken) {
}
