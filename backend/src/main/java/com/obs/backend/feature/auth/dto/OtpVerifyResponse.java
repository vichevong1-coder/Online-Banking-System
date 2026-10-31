package com.obs.backend.feature.auth.dto;

/** @param kycUploadToken short-lived token for uploading NID/selfie images; null if no KYC is pending */
public record OtpVerifyResponse(String kycUploadToken) {
}
