-- Opens 1 account each for the 5 demo customers from seed-demo-customers.sql (run that first).
-- Alternates SAVINGS/USD and CHECKING/KHR so both account types and currencies show up in
-- demo data for GET /accounts, GET /accounts/{id}/balance, and the statement PDF.
--
-- Run against the local dev database:
--   docker exec -i obs-postgres psql -U obs -d obs < backend/scripts/seed-demo-accounts.sql
--
-- Re-runnable: ON CONFLICT (account_number) DO NOTHING skips rows that already exist. Balance
-- starts at 0, matching what POST /accounts/requests actually does (US-014, auto-approved).

INSERT INTO accounts (id, user_id, account_number, account_type, currency, balance)
VALUES
    (gen_random_uuid(), (SELECT id FROM users WHERE phone = '+855-12-000-001'),
     '100000000001', 'SAVINGS', 'USD', 0),
    (gen_random_uuid(), (SELECT id FROM users WHERE phone = '+855-12-000-002'),
     '100000000002', 'CHECKING', 'KHR', 0),
    (gen_random_uuid(), (SELECT id FROM users WHERE phone = '+855-12-000-003'),
     '100000000003', 'SAVINGS', 'USD', 0),
    (gen_random_uuid(), (SELECT id FROM users WHERE phone = '+855-12-000-004'),
     '100000000004', 'CHECKING', 'KHR', 0),
    (gen_random_uuid(), (SELECT id FROM users WHERE phone = '+855-12-000-005'),
     '100000000005', 'SAVINGS', 'USD', 0)
ON CONFLICT (account_number) DO NOTHING;
