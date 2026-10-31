package com.obs.backend.feature.kyc.exception;

import org.springframework.http.HttpStatus;

public class IncompleteKycDocumentsException extends KycException {

    public IncompleteKycDocumentsException() {
        super(
                HttpStatus.CONFLICT,
                "KYC_DOCUMENTS_INCOMPLETE",
                "Front, back and selfie images are all required before approval");
    }

    public IncompleteKycDocumentsException(String message) {
        super(HttpStatus.CONFLICT, "KYC_DOCUMENTS_INCOMPLETE", message);
    }
}
