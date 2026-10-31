-- Camera-based KYC signup: a new customer is PENDING_APPROVAL until staff review the
-- NID photos and selfie. users.status is VARCHAR(20) with no CHECK constraint, and
-- 'PENDING_APPROVAL' (16 chars) fits, so only the review metadata needs new columns.
ALTER TABLE users
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_by      UUID REFERENCES users (id),
    ADD COLUMN reviewed_at      TIMESTAMPTZ;

-- One row per captured image. The bytes live encrypted on disk under storage_key; the
-- database keeps only metadata plus the SHA-256 of the plaintext so tampering or a
-- corrupted file is detectable at review time.
CREATE TABLE kyc_documents (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id),
    type         VARCHAR(20)  NOT NULL,
    storage_key  VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(50)  NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    sha256       CHAR(64)     NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_kyc_documents_user_type UNIQUE (user_id, type)
);

CREATE INDEX idx_users_status ON users (status);
