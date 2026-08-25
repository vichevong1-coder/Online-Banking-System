package com.obs.backend.feature.card.repository;

import com.obs.backend.feature.card.entity.Card;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Card> findByIdAndUserId(UUID id, UUID userId);
}
