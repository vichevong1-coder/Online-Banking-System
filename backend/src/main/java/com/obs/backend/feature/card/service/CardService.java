package com.obs.backend.feature.card.service;

import com.obs.backend.feature.card.dto.CardResponse;
import com.obs.backend.feature.card.dto.CreateCardRequest;
import com.obs.backend.feature.card.dto.UpdateCardRequest;
import java.util.List;
import java.util.UUID;

public interface CardService {
    CardResponse requestCard(UUID userId, CreateCardRequest request);
    List<CardResponse> listCards(UUID userId);
    CardResponse getCard(UUID userId, UUID cardId);
    CardResponse blockCard(UUID userId, UUID cardId);
    CardResponse unblockCard(UUID userId, UUID cardId);
    CardResponse updateCard(UUID userId, UUID cardId, UpdateCardRequest request);
}
