package com.obs.backend.feature.bill.repository;

import com.obs.backend.feature.bill.entity.RecurringBillPayment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringBillPaymentRepository extends JpaRepository<RecurringBillPayment, UUID> {
    List<RecurringBillPayment> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);
    Optional<RecurringBillPayment> findByIdAndUserId(UUID id, UUID userId);
}
