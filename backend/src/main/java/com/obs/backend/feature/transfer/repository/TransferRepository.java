package com.obs.backend.feature.transfer.repository;

import com.obs.backend.feature.transfer.entity.Transfer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Extends {@link JpaSpecificationExecutor} because the US-050 monitoring feed
 * filters on an optional combination of date range, amount, status and account.
 * Sprint 2 established that a {@code (:param IS NULL OR ...)} query fails
 * against Postgres when a bind parameter's only occurrence is inside an IS NULL
 * check — the driver cannot infer its type.
 */
public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

    boolean existsByReference(String reference);
}
