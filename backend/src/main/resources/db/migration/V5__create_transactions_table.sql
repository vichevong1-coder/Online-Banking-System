CREATE TABLE transactions (
    id            UUID PRIMARY KEY,
    account_id    UUID NOT NULL REFERENCES accounts(id),
    type          VARCHAR(20) NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    description   VARCHAR(255),
    balance_after NUMERIC(19, 4) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_account_id_created_at ON transactions (account_id, created_at DESC);
