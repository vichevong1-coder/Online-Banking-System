-- Debit card management (US-043, US-044, US-045)
CREATE TABLE cards (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL REFERENCES users(id),
    account_id            UUID NOT NULL REFERENCES accounts(id),
    card_holder_name      VARCHAR(100) NOT NULL,
    card_number_masked    VARCHAR(30) NOT NULL,
    card_number_last_four VARCHAR(4) NOT NULL,
    pin_hash              VARCHAR(255),
    card_type             VARCHAR(20) NOT NULL DEFAULT 'DEBIT',
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expiry_date           VARCHAR(5) NOT NULL,
    daily_limit           NUMERIC(19, 4) NOT NULL DEFAULT 1000.0000,
    per_transaction_limit NUMERIC(19, 4) NOT NULL DEFAULT 500.0000,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_user_id ON cards (user_id);
CREATE INDEX idx_cards_account_id ON cards (account_id);
CREATE INDEX idx_cards_status ON cards (status);
