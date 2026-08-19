package com.obs.backend.feature.beneficiary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A payee one customer has saved (US-029, US-030).
 *
 * <p>The destination is held as a bank code + account number rather than a
 * reference to an account row, because a beneficiary usually names an account at
 * another bank — the same shape {@code CreateExternalTransferRequest} takes.
 *
 * <p>Nothing links a beneficiary to a transfer: US-026 takes the destination
 * inline and has no beneficiary field, so this is an address book and only an
 * address book. Wiring the two together is not Sprint 4 work.
 */
@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The owning customer. Every read is scoped by it — see BeneficiaryServiceImpl. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** What the customer calls this payee, e.g. "Landlord". Editable (US-030). */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    /**
     * US-031 quick transfer, which is a Sprint 5 mobile story. The column exists
     * now because PATCH can set it for free; there is no favorites endpoint.
     */
    @Column(nullable = false)
    private boolean favorite;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Beneficiary() {
    }

    public Beneficiary(UUID userId, String displayName, String bankCode, String accountNumber) {
        this.userId = userId;
        this.displayName = displayName;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.favorite = false;
    }

    /**
     * US-030 is a partial update, so each field is set through its own call and
     * only when the request actually carried it. A single all-fields setter would
     * be the easy way to null out a name the client never sent.
     */
    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public void changeDestination(String bankCode, String accountNumber) {
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
