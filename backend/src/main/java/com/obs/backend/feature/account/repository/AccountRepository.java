package com.obs.backend.feature.account.repository;

import com.obs.backend.feature.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByAccountNumber(String accountNumber);
}
