-- A transfer is a first-class row, not two unlinked ledger entries.
--
-- Before this, a transfer was only visible as two `transactions` rows with no
-- link between them, no counterparty and no status. US-028 (receipt), US-050
-- (transfer monitoring feed) and US-053 (today's count and volume, which would
-- otherwise double-count both legs) all need the transfer itself.
--
-- to_account_id is NULL for an interbank transfer (US-026), where the
-- destination lives at another bank and is identified by external_ref instead.
CREATE TABLE transfers (
    id              UUID PRIMARY KEY,
    from_account_id UUID NOT NULL REFERENCES accounts(id),
    to_account_id   UUID REFERENCES accounts(id),
    external_ref    VARCHAR(64),
    amount          NUMERIC(19, 4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    reference       VARCHAR(35) NOT NULL UNIQUE,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Exactly one destination: an internal account or an external reference.
    CONSTRAINT chk_transfers_destination CHECK (
        (to_account_id IS NOT NULL AND external_ref IS NULL)
        OR (to_account_id IS NULL AND external_ref IS NOT NULL)
    ),
    CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfers_not_self CHECK (
        to_account_id IS NULL OR to_account_id <> from_account_id
    )
);

-- Drives the US-050 monitoring feed, which is ordered newest-first and filtered
-- by date range before anything else.
CREATE INDEX idx_transfers_created_at ON transfers (created_at DESC);
CREATE INDEX idx_transfers_from_account_id ON transfers (from_account_id, created_at DESC);
CREATE INDEX idx_transfers_to_account_id ON transfers (to_account_id, created_at DESC);
CREATE INDEX idx_transfers_status ON transfers (status);

-- Ties both ledger legs back to the transfer that produced them. Nullable
-- because deposits and withdrawals are transactions with no transfer.
ALTER TABLE transactions ADD COLUMN transfer_id UUID REFERENCES transfers(id);

CREATE INDEX idx_transactions_transfer_id ON transactions (transfer_id);
