package com.obs.backend.feature.card.service.impl;

import com.obs.backend.feature.account.entity.Account;
import com.obs.backend.feature.account.exception.AccountNotFoundException;
import com.obs.backend.feature.account.repository.AccountRepository;
import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.card.dto.CardResponse;
import com.obs.backend.feature.card.dto.CreateCardRequest;
import com.obs.backend.feature.card.dto.UpdateCardRequest;
import com.obs.backend.feature.card.entity.Card;
import com.obs.backend.feature.card.entity.CardStatus;
import com.obs.backend.feature.card.entity.CardType;
import com.obs.backend.feature.card.exception.CardNotFoundException;
import com.obs.backend.feature.card.repository.CardRepository;
import com.obs.backend.feature.card.service.CardService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardServiceImpl implements CardService {

    private static final String BIN = "411122";
    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("1000.0000");
    private static final BigDecimal DEFAULT_PER_TX_LIMIT = new BigDecimal("500.0000");
    private static final DateTimeFormatter EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public CardServiceImpl(
            CardRepository cardRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public CardResponse requestCard(UUID userId, CreateCardRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.accountId(), userId)
                .orElseThrow(AccountNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        String holderName = request.cardHolderName() != null && !request.cardHolderName().isBlank()
                ? request.cardHolderName().trim().toUpperCase()
                : (user.getFirstName() + " " + user.getLastName()).toUpperCase();

        int lastFourDigits = secureRandom.nextInt(9000) + 1000;
        String lastFour = String.valueOf(lastFourDigits);
        String maskedCardNumber = BIN + "******" + lastFour;

        String expiryDate = LocalDate.now().plusYears(3).format(EXPIRY_FORMATTER);

        String pinHash = request.pin() != null && !request.pin().isBlank()
                ? passwordEncoder.encode(request.pin())
                : null;

        Card card = new Card(
                userId,
                account.getId(),
                holderName,
                maskedCardNumber,
                lastFour,
                pinHash,
                CardType.DEBIT,
                CardStatus.ACTIVE,
                expiryDate,
                DEFAULT_DAILY_LIMIT,
                DEFAULT_PER_TX_LIMIT
        );

        card = cardRepository.save(card);
        return toResponse(card, account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> listCards(UUID userId) {
        return cardRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getCard(UUID userId, UUID cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CardNotFoundException::new);
        return toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse blockCard(UUID userId, UUID cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CardNotFoundException::new);
        card.block();
        card = cardRepository.save(card);
        return toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse unblockCard(UUID userId, UUID cardId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CardNotFoundException::new);
        card.unblock();
        card = cardRepository.save(card);
        return toResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCard(UUID userId, UUID cardId, UpdateCardRequest request) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(CardNotFoundException::new);

        if (request.pin() != null && !request.pin().isBlank()) {
            card.setPinHash(passwordEncoder.encode(request.pin()));
        }
        if (request.dailyLimit() != null) {
            card.setDailyLimit(request.dailyLimit());
        }
        if (request.perTransactionLimit() != null) {
            card.setPerTransactionLimit(request.perTransactionLimit());
        }

        card = cardRepository.save(card);
        return toResponse(card);
    }

    private CardResponse toResponse(Card card) {
        Account account = accountRepository.findById(card.getAccountId()).orElse(null);
        return toResponse(card, account);
    }

    private CardResponse toResponse(Card card, Account account) {
        return new CardResponse(
                card.getId(),
                card.getAccountId(),
                account != null ? account.getAccountNumber() : "Unknown Account",
                card.getCardHolderName(),
                card.getCardNumberMasked(),
                card.getCardNumberLastFour(),
                card.getCardType(),
                card.getStatus(),
                card.getExpiryDate(),
                card.getDailyLimit(),
                card.getPerTransactionLimit(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
