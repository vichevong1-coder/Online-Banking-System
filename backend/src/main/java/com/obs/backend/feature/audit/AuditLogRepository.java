package com.obs.backend.feature.audit;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(cast(:startDate as timestamp) IS NULL OR a.createdAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR a.createdAt <= :endDate)")
    Page<AuditLog> findByFilters(
            @Param("actionType") String actionType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);
}
