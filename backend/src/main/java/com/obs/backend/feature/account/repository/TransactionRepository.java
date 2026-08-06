package com.obs.backend.feature.account.repository;

import com.obs.backend.feature.account.entity.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// US-017/US-018's date / type / amount filters are all optional, so listing goes through
// JpaSpecificationExecutor (see AccountServiceImpl) — a plain @Query with "(:param IS NULL OR
// ...)" branches fails against Postgres: the driver can't infer a bind parameter's type when its
// only occurrence is inside an IS NULL check with no value bound.
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    // US-019: statements pull every transaction in a date range, unpaginated. Exclusive upper
    // bound (LessThan, not Between) — the caller passes the start of the day *after* the last day
    // of the period, so a transaction stamped exactly at that boundary isn't double-counted into
    // the next statement.
    List<Transaction> findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            UUID accountId, Instant fromDate, Instant toDateExclusive);
}
