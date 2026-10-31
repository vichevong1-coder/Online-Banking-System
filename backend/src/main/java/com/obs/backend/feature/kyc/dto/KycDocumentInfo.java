package com.obs.backend.feature.kyc.dto;

import com.obs.backend.feature.kyc.entity.KycDocumentType;
import java.time.Instant;

public record KycDocumentInfo(KycDocumentType type, long sizeBytes, String sha256, Instant uploadedAt) {
}
