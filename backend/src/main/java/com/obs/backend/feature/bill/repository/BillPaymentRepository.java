package com.obs.backend.feature.bill.repository;

import com.obs.backend.feature.bill.entity.BillPayment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {
    Page<BillPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<BillPayment> findByIdAndUserId(UUID id, UUID userId);
}
