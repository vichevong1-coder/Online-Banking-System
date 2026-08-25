package com.obs.backend.feature.bill.repository;

import com.obs.backend.feature.bill.entity.BillCategory;
import com.obs.backend.feature.bill.entity.BillProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillProviderRepository extends JpaRepository<BillProvider, UUID> {
    List<BillProvider> findByActiveTrueOrderByNameAsc();
    List<BillProvider> findByActiveTrueAndCategoryOrderByNameAsc(BillCategory category);
    Optional<BillProvider> findByNameIgnoreCase(String name);
}
