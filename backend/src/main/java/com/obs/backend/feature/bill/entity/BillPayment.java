package com.obs.backend.feature.bill.entity;

import com.obs.backend.feature.account.entity.Currency;
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
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "bill_account_number", nullable = false)
    private String billAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BillPayment() {}

    public BillPayment(
            UUID userId,
            UUID providerId,
            UUID accountId,
            String billAccountNumber,
            BigDecimal amount,
            Currency currency,
            UUID transferId,
            String reference,
            String status) {
        this.userId = userId;
        this.providerId = providerId;
        this.accountId = accountId;
        this.billAccountNumber = billAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.transferId = transferId;
        this.reference = reference;
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProviderId() { return providerId; }
    public UUID getAccountId() { return accountId; }
    public String getBillAccountNumber() { return billAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public UUID getTransferId() { return transferId; }
    public String getReference() { return reference; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
