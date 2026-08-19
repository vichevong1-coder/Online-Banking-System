package com.obs.backend.feature.qr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A payee named by a merchant QR code (US-034).
 *
 * <p>Not a user: a merchant never logs in. It is a merchant code, a name to show
 * and an ordinary account to settle into, which is all a payee needs to be for a
 * payment to be a plain internal transfer.
 */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** What a merchant QR payload carries as its target, e.g. {@code MERCH-ANGKOR}. Unique. */
    @Column(name = "merchant_code", nullable = false, unique = true, updatable = false)
    private String merchantCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** A real accounts row — the credit leg of a merchant payment lands here. */
    @Column(name = "settlement_account_id", nullable = false)
    private UUID settlementAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Merchant() {
    }

    public Merchant(String merchantCode, String displayName, UUID settlementAccountId, MerchantStatus status) {
        this.merchantCode = merchantCode;
        this.displayName = displayName;
        this.settlementAccountId = settlementAccountId;
        this.status = status;
    }

    public boolean declinesEverything() {
        return status == MerchantStatus.ALWAYS_DECLINES;
    }

    public UUID getId() {
        return id;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getSettlementAccountId() {
        return settlementAccountId;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
