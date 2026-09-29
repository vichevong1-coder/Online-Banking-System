package com.obs.backend.security.jwt;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {
    @Id
    @Column(name = "token_id")
    private String tokenId;

    @CreationTimestamp
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    protected RevokedToken() {}

    public RevokedToken(String tokenId) {
        this.tokenId = tokenId;
    }
    
    public Instant getRevokedAt() {
        return revokedAt;
    }
}
