package com.obs.backend.feature.auth.repository;

import com.obs.backend.feature.auth.entity.OtpCode;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId, OtpPurpose purpose);
}
