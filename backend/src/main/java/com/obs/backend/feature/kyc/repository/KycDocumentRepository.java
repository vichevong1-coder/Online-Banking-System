package com.obs.backend.feature.kyc.repository;

import com.obs.backend.feature.kyc.entity.KycDocument;
import com.obs.backend.feature.kyc.entity.KycDocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    Optional<KycDocument> findByUserIdAndType(UUID userId, KycDocumentType type);

    List<KycDocument> findByUserIdOrderByTypeAsc(UUID userId);

    long countByUserId(UUID userId);
}
