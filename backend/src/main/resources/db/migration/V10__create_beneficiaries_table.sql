-- Saved payees a customer transfers to (US-029, US-030).
--
-- A beneficiary is a customer-owned address book entry, not a party to any
-- transfer: US-026 deliberately takes a raw bank code + account number and the
-- transfers table has no beneficiary_id. Nothing here is referenced by a
-- transfer row, so deleting a beneficiary can never orphan a past transfer.
--
-- bank_code and account_number carry the same shapes as
-- CreateExternalTransferRequest, so a saved beneficiary is always something the
-- interbank endpoint would accept.
CREATE TABLE beneficiaries (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    display_name   VARCHAR(100) NOT NULL,
    bank_code      VARCHAR(11) NOT NULL,
    account_number VARCHAR(34) NOT NULL,

    -- US-031 (favorites / quick transfer) is Flutter-only and ships in Sprint 5.
    -- The flag lives here now because a boolean column plus a PATCH field is the
    -- whole backend cost of it; there is no favorites endpoint and no screen.
    favorite       BOOLEAN NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- One customer cannot save the same destination twice. Scoped to the user:
    -- two customers paying the same landlord is normal, not a duplicate.
    CONSTRAINT uq_beneficiaries_user_destination UNIQUE (user_id, bank_code, account_number)
);

-- The only read path is "this customer's list, newest first" (US-029).
CREATE INDEX idx_beneficiaries_user_id ON beneficiaries (user_id, created_at DESC);
