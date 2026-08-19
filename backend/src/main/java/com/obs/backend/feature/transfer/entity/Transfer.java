package com.obs.backend.feature.transfer.entity;

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

/**
 * One money movement, owning both of its ledger legs.
 *
 * <p>A transfer exists as a row in its own right so that a receipt (US-028), the
 * admin monitoring feed (US-050) and the daily volume KPI (US-053) all have a
 * single thing to point at. Reading the two {@code transactions} legs instead
 * would double-count every internal transfer.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    /** Null for an interbank transfer, where {@link #externalRef} names the destination instead. */
    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    /** Customer-facing identifier printed on the receipt. Unique. */
    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    private Transfer(
            UUID fromAccountId,
            UUID toAccountId,
            String externalRef,
            BigDecimal amount,
            Currency currency,
            TransferStatus status,
            String reference,
            String description) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.externalRef = externalRef;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.reference = reference;
        this.description = description;
    }

    /** Between two accounts held at this bank (US-025). */
    public static Transfer internal(
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            Currency currency,
            String reference,
            String description) {
        return new Transfer(
                fromAccountId,
                toAccountId,
                null,
                amount,
                currency,
                TransferStatus.COMPLETED,
                reference,
                description);
    }

    /**
     * To an account at another bank (US-026), simulated for the demo. Starts
     * PENDING because the destination is not ours to credit.
     */
    public static Transfer external(
            UUID fromAccountId,
            String externalRef,
            BigDecimal amount,
            Currency currency,
            String reference,
            String description) {
        return new Transfer(
                fromAccountId,
                null,
                externalRef,
                amount,
                currency,
                TransferStatus.PENDING,
                reference,
                description);
    }

    public void markCompleted() {
        this.status = TransferStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = TransferStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
