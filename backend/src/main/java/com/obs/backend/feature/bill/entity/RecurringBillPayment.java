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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "recurring_bill_payments")
public class RecurringBillPayment {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency frequency;

    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringBillPayment() {}

    public RecurringBillPayment(
            UUID userId,
            UUID providerId,
            UUID accountId,
            String billAccountNumber,
            BigDecimal amount,
            Currency currency,
            PaymentFrequency frequency,
            LocalDate nextPaymentDate) {
        this.userId = userId;
        this.providerId = providerId;
        this.accountId = accountId;
        this.billAccountNumber = billAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
        this.nextPaymentDate = nextPaymentDate;
        this.active = true;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getProviderId() { return providerId; }
    public UUID getAccountId() { return accountId; }
    public String getBillAccountNumber() { return billAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public PaymentFrequency getFrequency() { return frequency; }
    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public boolean isActive() { return active; }
    public void cancel() { this.active = false; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
