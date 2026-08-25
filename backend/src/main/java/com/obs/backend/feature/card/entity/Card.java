package com.obs.backend.feature.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Column(name = "card_number_masked", nullable = false)
    private String cardNumberMasked;

    @Column(name = "card_number_last_four", nullable = false)
    private String cardNumberLastFour;

    @Column(name = "pin_hash")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType = CardType.DEBIT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "expiry_date", nullable = false)
    private String expiryDate;

    @Column(name = "daily_limit", nullable = false)
    private BigDecimal dailyLimit;

    @Column(name = "per_transaction_limit", nullable = false)
    private BigDecimal perTransactionLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Card() {}

    public Card(
            UUID userId,
            UUID accountId,
            String cardHolderName,
            String cardNumberMasked,
            String cardNumberLastFour,
            String pinHash,
            CardType cardType,
            CardStatus status,
            String expiryDate,
            BigDecimal dailyLimit,
            BigDecimal perTransactionLimit) {
        this.userId = userId;
        this.accountId = accountId;
        this.cardHolderName = cardHolderName;
        this.cardNumberMasked = cardNumberMasked;
        this.cardNumberLastFour = cardNumberLastFour;
        this.pinHash = pinHash;
        this.cardType = cardType;
        this.status = status;
        this.expiryDate = expiryDate;
        this.dailyLimit = dailyLimit;
        this.perTransactionLimit = perTransactionLimit;
    }

    public void block() { this.status = CardStatus.BLOCKED; }
    public void unblock() { this.status = CardStatus.ACTIVE; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public void setPerTransactionLimit(BigDecimal perTransactionLimit) { this.perTransactionLimit = perTransactionLimit; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getAccountId() { return accountId; }
    public String getCardHolderName() { return cardHolderName; }
    public String getCardNumberMasked() { return cardNumberMasked; }
    public String getCardNumberLastFour() { return cardNumberLastFour; }
    public String getPinHash() { return pinHash; }
    public CardType getCardType() { return cardType; }
    public CardStatus getStatus() { return status; }
    public String getExpiryDate() { return expiryDate; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public BigDecimal getPerTransactionLimit() { return perTransactionLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
