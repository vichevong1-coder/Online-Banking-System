package com.obs.backend.feature.beneficiary.repository;

import com.obs.backend.feature.beneficiary.entity.Beneficiary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    /** US-029 list: the caller's own saved payees, newest first. */
    Page<Beneficiary> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Every single-row lookup goes through this rather than findById: scoping the
     * query by owner is what turns "somebody else's beneficiary" into a 404
     * instead of a 403 (the Sprint 2 rule), without a separate ownership branch
     * that could be forgotten on one of the two endpoints.
     */
    Optional<Beneficiary> findByIdAndUserId(UUID id, UUID userId);

    /** Backs the pre-check behind BENEFICIARY_ALREADY_EXISTS; the UNIQUE constraint backs it up in turn. */
    boolean existsByUserIdAndBankCodeAndAccountNumber(UUID userId, String bankCode, String accountNumber);
}
