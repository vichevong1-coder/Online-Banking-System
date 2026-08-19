package com.obs.backend.feature.transfer.repository;

import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Extends {@link JpaSpecificationExecutor} because the US-050 monitoring feed
 * filters on an optional combination of date range, amount, status and account.
 * Sprint 2 established that a {@code (:param IS NULL OR ...)} query fails
 * against Postgres when a bind parameter's only occurrence is inside an IS NULL
 * check — the driver cannot infer its type.
 */
public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

    boolean existsByReference(String reference);

    /** US-028 history: every transfer the caller sent or received, newest first. */
    Page<Transfer> findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDesc(
            Collection<UUID> fromAccountIds, Collection<UUID> toAccountIds, Pageable pageable);

    /**
     * US-027 daily cap. Counts money <em>sent</em> only — matching on the
     * destination too would double-count every own-accounts transfer, because
     * both of its legs belong to the same customer.
     *
     * <p>The window is a UTC day, the bucketing Sprint 2 established for
     * date-scoped queries.
     */
    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.fromAccountId IN :accountIds
              AND t.status = :status
              AND t.currency = :currency
              AND t.createdAt >= :from
              AND t.createdAt < :toExclusive
            """)
    BigDecimal sumSentAmount(
            @Param("accountIds") Collection<UUID> accountIds,
            @Param("status") TransferStatus status,
            @Param("currency") com.obs.backend.feature.account.entity.Currency currency,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * US-053 dashboard overview KPI: count of today's completed transfers across
     * the whole system.
     */
    @Query(
            """
            SELECT COUNT(t)
            FROM Transfer t
            WHERE t.status = :status
              AND t.createdAt >= :from
              AND t.createdAt < :toExclusive
            """)
    long countByStatusAndCreatedAtBetween(
            @Param("status") TransferStatus status,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * US-053 dashboard overview KPI: sum of today's completed transfer volume in USD.
     */
    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.status = :status
              AND t.currency = :currency
              AND t.createdAt >= :from
              AND t.createdAt < :toExclusive
            """)
    BigDecimal sumAmountByStatusAndCurrencyAndCreatedAtBetween(
            @Param("status") TransferStatus status,
            @Param("currency") com.obs.backend.feature.account.entity.Currency currency,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);
}

