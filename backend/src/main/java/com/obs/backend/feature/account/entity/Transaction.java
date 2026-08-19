package com.obs.backend.feature.account.entity;

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

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    private String description;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    // Ties this leg back to the transfer that produced it. Null for a
    // transaction with no transfer behind it, such as a deposit.
    @Column(name = "transfer_id")
    private UUID transferId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {
    }

    public Transaction(
            UUID accountId,
            TransactionType type,
            BigDecimal amount,
            Currency currency,
            String description,
            BigDecimal balanceAfter) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.balanceAfter = balanceAfter;
    }

    public Transaction(
            UUID accountId,
            TransactionType type,
            BigDecimal amount,
            Currency currency,
            String description,
            BigDecimal balanceAfter,
            UUID transferId) {
        this(accountId, type, amount, currency, description, balanceAfter);
        this.transferId = transferId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
