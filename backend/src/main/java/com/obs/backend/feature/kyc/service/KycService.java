package com.obs.backend.feature.kyc.service;

import com.obs.backend.common.dto.PageResponse;
import com.obs.backend.feature.kyc.dto.KycApplicationResponse;
import com.obs.backend.feature.kyc.dto.KycImage;
import com.obs.backend.feature.kyc.dto.KycUploadResponse;
import com.obs.backend.feature.kyc.dto.PendingKycSummaryResponse;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface KycService {

    /**
     * Stores one captured image for the customer named by the upload token. Uploading the same
     * type again replaces the earlier image (a retake).
     *
     * @param authorizationHeader the raw {@code Authorization} header carrying the KYC upload token
     */
    KycUploadResponse upload(String authorizationHeader, KycDocumentType type, byte[] bytes);

    /** Applications with all required images uploaded, oldest first. */
    PageResponse<PendingKycSummaryResponse> listPending(Pageable pageable);

    KycApplicationResponse getApplication(UUID userId);

    /** Decrypts one image for a reviewer. Every call is audit-logged. */
    KycImage getImage(UUID userId, KycDocumentType type);

    KycApplicationResponse approve(UUID userId, UUID reviewerId);

    KycApplicationResponse reject(UUID userId, UUID reviewerId, String reason);
}
