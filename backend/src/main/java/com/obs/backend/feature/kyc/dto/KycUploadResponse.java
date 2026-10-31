package com.obs.backend.feature.kyc.dto;

import com.obs.backend.feature.kyc.entity.KycDocumentType;
import java.util.List;

/** {@code missing} lets the app know when the application is complete without a second call. */
public record KycUploadResponse(KycDocumentType type, List<KycDocumentType> missing) {
}
