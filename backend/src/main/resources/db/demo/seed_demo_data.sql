-- Demo data for local development. NOT a Flyway migration, on purpose.
--
--   docker exec -i obs-postgres psql -U obs -d obs \
--       < backend/src/main/resources/db/demo/seed_demo_data.sql
--
-- Why not a migration: `application.properties` pins spring.profiles.active=dev
-- and there are no test resources overriding it, so anything Flyway picks up
-- also loads during tests and in CI. This script stays outside
-- spring.flyway.locations so it only ever runs when you run it by hand.
--
-- Re-runnable: it deletes its own rows (fixed UUIDs) before inserting, so you
-- can reset to a known demo state at any time without wiping the database.
--
-- Every password below is bcrypt("ChangeMe123!") — the same throwaway local
-- hash used for the bootstrap admin in V2. Local demo only. See
-- .claude/demo-customers.md and .claude/demo-admin.md.

BEGIN;

-- Clean out previous demo rows, children first (FKs).
DELETE FROM transactions WHERE account_id IN (
    SELECT id FROM accounts WHERE user_id IN (
        SELECT id FROM users WHERE id::text LIKE 'd0000000-%'));
DELETE FROM otp_codes WHERE user_id IN (
    SELECT id FROM users WHERE id::text LIKE 'd0000000-%');
DELETE FROM accounts WHERE user_id IN (
    SELECT id FROM users WHERE id::text LIKE 'd0000000-%');
DELETE FROM users WHERE id::text LIKE 'd0000000-%';

-- ---------------------------------------------------------------- customers
-- email stays NULL: customers are identified by phone (US-007/US-009) and the
-- KYC form collects no email address. phone_verified = TRUE so they can log in
-- without walking the OTP flow first — except Vibol, see below.
INSERT INTO users (id, first_name, last_name, password_hash, email, nid_number,
                   nid_expiry_date, date_of_birth, gender, phone, role, status,
                   phone_verified, created_at) VALUES
('d0000000-0000-0000-0000-000000000001', 'Sophea', 'Chan',
 '$2a$10$CPvFtm.yHrdrzSprtnnBueL/7ElKS8l65dN1LvRN1SXvgGiHxc/wy', NULL,
 '012345678', '2030-04-12', '1994-03-08', 'FEMALE', '+85512000001',
 'CUSTOMER', 'ACTIVE', TRUE, now() - INTERVAL '95 days'),
('d0000000-0000-0000-0000-000000000002', 'Dara', 'Sok',
 '$2a$10$CPvFtm.yHrdrzSprtnnBueL/7ElKS8l65dN1LvRN1SXvgGiHxc/wy', NULL,
 '023456789', '2029-11-30', '1988-07-22', 'MALE', '+85512000002',
 'CUSTOMER', 'ACTIVE', TRUE, now() - INTERVAL '92 days'),
('d0000000-0000-0000-0000-000000000003', 'Nita', 'Pich',
 '$2a$10$CPvFtm.yHrdrzSprtnnBueL/7ElKS8l65dN1LvRN1SXvgGiHxc/wy', NULL,
 '034567890', '2031-01-15', '1999-12-02', 'FEMALE', '+85512000003',
 'CUSTOMER', 'ACTIVE', TRUE, now() - INTERVAL '80 days'),
-- Deliberately SUSPENDED: gives US-006's login-path enforcement and US-048's
-- admin status actions something real to demo. This account cannot log in.
('d0000000-0000-0000-0000-000000000004', 'Vibol', 'Keo',
 '$2a$10$CPvFtm.yHrdrzSprtnnBueL/7ElKS8l65dN1LvRN1SXvgGiHxc/wy', NULL,
 '045678901', '2028-06-08', '1991-09-17', 'MALE', '+85512000004',
 'CUSTOMER', 'SUSPENDED', TRUE, now() - INTERVAL '30 days');

-- ----------------------------------------------------------------- accounts
-- Demo account numbers use the 9000000000xx block so they cannot collide with
-- numbers the app generates for accounts opened through the UI.
-- Each balance below equals the balance_after of that account's last transaction.
INSERT INTO accounts (id, user_id, account_number, account_type, currency, balance, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
 '900000000001', 'SAVINGS',  'USD',    1500.5000, now() - INTERVAL '95 days'),
('a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001',
 '900000000002', 'CHECKING', 'KHR', 1750000.0000, now() - INTERVAL '90 days'),
('a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002',
 '900000000003', 'SAVINGS',  'USD',    3625.2500, now() - INTERVAL '92 days'),
('a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000003',
 '900000000004', 'CHECKING', 'USD',     550.0000, now() - INTERVAL '80 days'),
('a0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000003',
 '900000000005', 'SAVINGS',  'KHR', 5000000.0000, now() - INTERVAL '78 days'),
('a0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000004',
 '900000000006', 'SAVINGS',  'USD',     250.0000, now() - INTERVAL '30 days');

-- ------------------------------------------------------------- transactions
-- Spread across ~3 months so US-017's list, US-018's date/type/amount filters
-- and US-019's PDF statement all have something non-trivial to page through.
INSERT INTO transactions (id, account_id, type, amount, currency, description, balance_after, created_at) VALUES
-- 900000000001 — Sophea, USD savings
('7a000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'DEPOSIT',       1000.0000, 'USD', 'Opening deposit',        1000.0000, now() - INTERVAL '60 days'),
('7a000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'DEPOSIT',        500.0000, 'USD', 'Salary',                 1500.0000, now() - INTERVAL '45 days'),
('7a000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'WITHDRAWAL',     200.0000, 'USD', 'ATM withdrawal',         1300.0000, now() - INTERVAL '30 days'),
('7a000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'TRANSFER_OUT',   150.0000, 'USD', 'Transfer to Dara Sok',   1150.0000, now() - INTERVAL '20 days'),
('7a000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'DEPOSIT',        350.5000, 'USD', 'Refund',                 1500.5000, now() - INTERVAL '10 days'),
-- 900000000002 — Sophea, KHR checking
('7a000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000002', 'DEPOSIT',    2000000.0000, 'KHR', 'Opening deposit',     2000000.0000, now() - INTERVAL '50 days'),
('7a000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000002', 'WITHDRAWAL',  500000.0000, 'KHR', 'Market',              1500000.0000, now() - INTERVAL '25 days'),
('7a000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000002', 'TRANSFER_IN',  250000.0000, 'KHR', 'From Nita Pich',      1750000.0000, now() - INTERVAL '5 days'),
-- 900000000003 — Dara, USD savings
('7a000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000003', 'DEPOSIT',       5000.0000, 'USD', 'Opening deposit',        5000.0000, now() - INTERVAL '90 days'),
('7a000000-0000-0000-0000-00000000000a', 'a0000000-0000-0000-0000-000000000003', 'TRANSFER_OUT',  1200.0000, 'USD', 'Rent',                   3800.0000, now() - INTERVAL '40 days'),
('7a000000-0000-0000-0000-00000000000b', 'a0000000-0000-0000-0000-000000000003', 'WITHDRAWAL',     300.0000, 'USD', 'ATM withdrawal',         3500.0000, now() - INTERVAL '15 days'),
('7a000000-0000-0000-0000-00000000000c', 'a0000000-0000-0000-0000-000000000003', 'DEPOSIT',        125.2500, 'USD', 'Interest',               3625.2500, now() - INTERVAL '3 days'),
-- 900000000004 — Nita, USD checking
('7a000000-0000-0000-0000-00000000000d', 'a0000000-0000-0000-0000-000000000004', 'DEPOSIT',        800.0000, 'USD', 'Opening deposit',         800.0000, now() - INTERVAL '70 days'),
('7a000000-0000-0000-0000-00000000000e', 'a0000000-0000-0000-0000-000000000004', 'TRANSFER_IN',    150.0000, 'USD', 'From Sophea Chan',        950.0000, now() - INTERVAL '35 days'),
('7a000000-0000-0000-0000-00000000000f', 'a0000000-0000-0000-0000-000000000004', 'WITHDRAWAL',     400.0000, 'USD', 'Utilities',               550.0000, now() - INTERVAL '12 days'),
-- 900000000005 — Nita, KHR savings
('7a000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000005', 'DEPOSIT',    4000000.0000, 'KHR', 'Opening deposit',     4000000.0000, now() - INTERVAL '55 days'),
('7a000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000005', 'DEPOSIT',    1000000.0000, 'KHR', 'Savings top-up',      5000000.0000, now() - INTERVAL '21 days'),
-- 900000000006 — Vibol (suspended), USD savings
('7a000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000006', 'DEPOSIT',        250.0000, 'USD', 'Opening deposit',         250.0000, now() - INTERVAL '8 days');

COMMIT;
