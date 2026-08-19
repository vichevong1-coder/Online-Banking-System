package com.obs.backend.feature.account.repository;

import com.obs.backend.feature.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    /**
     * US-033: resolves the account number a personal QR payload names. Not
     * owner-scoped — a personal QR is paid by somebody else, which is the whole
     * point of it.
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
