package com.obs.backend.feature.kyc.exception;

import org.springframework.http.HttpStatus;

public class KycException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private KycException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static KycException invalidUploadToken() {
        return new KycException(HttpStatus.UNAUTHORIZED, "INVALID_OR_EXPIRED_UPLOAD_TOKEN",
                "Upload token is invalid or expired");
    }

    public static KycException invalidDocument(String message) {
        return new KycException(HttpStatus.BAD_REQUEST, "INVALID_KYC_DOCUMENT", message);
    }

    public static KycException uploadNotAllowed() {
        return new KycException(HttpStatus.CONFLICT, "KYC_UPLOAD_NOT_ALLOWED",
                "This account is not awaiting document review");
    }

    public static KycException applicationNotFound() {
        return new KycException(HttpStatus.NOT_FOUND, "KYC_APPLICATION_NOT_FOUND", "KYC application not found");
    }

    public static KycException documentNotFound() {
        return new KycException(HttpStatus.NOT_FOUND, "KYC_DOCUMENT_NOT_FOUND", "KYC document not found");
    }

    public static KycException notPending() {
        return new KycException(HttpStatus.CONFLICT, "KYC_NOT_PENDING", "Application is not pending approval");
    }

    public static KycException incomplete() {
        return new KycException(HttpStatus.CONFLICT, "KYC_DOCUMENTS_INCOMPLETE",
                "Front, back and selfie images are all required before approval");
    }

    public static KycException reviewRequired() {
        return new KycException(HttpStatus.CONFLICT, "KYC_REVIEW_REQUIRED",
                "Use the KYC approve/reject endpoints for accounts in review");
    }
}
