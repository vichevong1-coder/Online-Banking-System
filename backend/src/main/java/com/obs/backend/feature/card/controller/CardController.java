package com.obs.backend.feature.card.controller;

import com.obs.backend.feature.card.dto.CardResponse;
import com.obs.backend.feature.card.dto.CreateCardRequest;
import com.obs.backend.feature.card.dto.UpdateCardRequest;
import com.obs.backend.feature.card.service.CardService;
import com.obs.backend.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;
    private final CurrentUserProvider currentUserProvider;

    public CardController(CardService cardService, CurrentUserProvider currentUserProvider) {
        this.cardService = cardService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse requestCard(@Valid @RequestBody CreateCardRequest request) {
        return cardService.requestCard(currentUserProvider.currentUserId(), request);
    }

    @GetMapping
    public List<CardResponse> listCards() {
        return cardService.listCards(currentUserProvider.currentUserId());
    }

    @GetMapping("/{id}")
    public CardResponse getCard(@PathVariable UUID id) {
        return cardService.getCard(currentUserProvider.currentUserId(), id);
    }

    @PostMapping("/{id}/block")
    public CardResponse blockCard(@PathVariable UUID id) {
        return cardService.blockCard(currentUserProvider.currentUserId(), id);
    }

    @PostMapping("/{id}/unblock")
    public CardResponse unblockCard(@PathVariable UUID id) {
        return cardService.unblockCard(currentUserProvider.currentUserId(), id);
    }

    @PatchMapping("/{id}")
    public CardResponse updateCard(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCardRequest request) {
        return cardService.updateCard(currentUserProvider.currentUserId(), id, request);
    }
}
