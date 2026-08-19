-- Optimistic locking on account balances (US-027).
--
-- Without this, two concurrent debits read the same balance and both write
-- their own result. The second write silently discards the first — a lost
-- update that lets the demo create money by firing two transfers at once.
--
-- NOT NULL DEFAULT 0 matters: accounts already exist (including every seeded
-- demo account), and Hibernate throws on a null version for a pre-existing
-- row. That would surface at the first transfer, not at migration time.
ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
