package com.obs.backend.feature.auth.repository;

import com.obs.backend.feature.auth.entity.LoginAttempt;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    long countBySuccessFalse();

    long countBySuccessFalseAndCreatedAtAfter(Instant after);

    long countByIdentifierAndSuccessFalseAndCreatedAtAfter(String identifier, Instant after);
}
