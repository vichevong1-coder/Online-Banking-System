# Sprint 4 To-Do List — Money Movement (backend) & Transfer Monitoring

Weeks 7–8. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

> **Backend and admin only.** The mobile screens for every flow in this sprint (US-025, US-026,
> US-028–US-033, US-035) land in Sprint 5, one sprint behind their APIs. Nothing in `mobile/` is in
> scope here.

---

## Before you start

- **Run the demo seed.** Same as Sprint 3 — the database has no data of its own after a rebuild:
  ```bash
  docker exec -i obs-postgres psql -U obs -d obs \
      < backend/src/main/resources/db/demo/seed_demo_data.sql
  ```
  Credentials for the seeded accounts are in `.claude/demo-admin.md` and `.claude/demo-customers.md`.
- **Write the US-034 merchant QR spec.** Still listed under "Still to be written" in sprint-plan.md.
  Minimum viable: seed three demo merchants; scanning a merchant QR settles instantly; one seeded
  merchant always declines so the failure path is demoable. **US-034 cannot start until this exists.**
- **Extend the demo seed with beneficiaries and a few transfers** once the tables land, or US-050's
  feed and US-053's tiles both render empty on a fresh database.

## Decisions already made — do not relitigate mid-implementation

- **Cross-currency transfers are rejected, not converted.** There is no rate table and no conversion
  service. US-027 rejects a transfer whose source and destination currencies differ with
  `400 CURRENCY_MISMATCH`. Multi-currency stays a display feature.
- **US-053's volume KPI reports in USD.** `AdminKpiServiceImpl` already returns
  `displayCurrency = "USD"` alongside the stubbed tiles — the KPI sums USD transfers only, it does
  not convert KHR ones in.
- **Over-limit transfers are a hard block.** US-027 rejects with a `400`; nothing is flagged for
  review. US-040 (fraud/risk check) is cut from the plan, so US-027 carries transfer protection alone.

## Backend prerequisites

Neither is a story; both block US-025 and must land first.

- [x] **`transfers` as a first-class row.** The `transactions` table is a per-account ledger
      (`account_id`, `amount`, `balance_after`) with no `transfer_id`, no counterparty and no status,
      so a transfer today is two unlinked rows. Add `V8__create_transfers_table.sql` creating
      `transfers (id, from_account_id, to_account_id NULL, external_ref, amount, currency, status,
      reference UNIQUE, created_at)` plus `transactions.transfer_id`, and have **one**
      `@Transactional` service method write the transfer and both legs together. US-028 (receipt),
      US-050 (feed) and US-053 (count & volume, which would otherwise double-count) all depend on it.
- [x] **Optimistic locking on `accounts.balance`.** `Account` has no `@Version` field today and
      nothing takes a row lock, so two concurrent debits read the same balance and both write their
      own result — a lost update that lets the demo create money. Needs a `@Version` column +
      migration, a `409` (or a bounded retry) on `OptimisticLockingFailureException`, and a test that
      fires two simultaneous transfers at one account and proves it cannot overdraw.

## API endpoints (`backend/`)

Customer endpoints are scoped to the caller's own accounts, following the Sprint 2 rule: another
customer's account ID returns `404 ACCOUNT_NOT_FOUND`, not `403`, so IDs can't probe ownership.

### Transfers (US-025–US-028)

- [ ] `POST /transfers` — between own accounts (US-025)
- [ ] `POST /transfers/external` — to another bank, simulated (US-026)
- [ ] Limits & validation rules — daily cap, per-transfer cap, insufficient funds,
      `400 CURRENCY_MISMATCH` (US-027)
- [ ] `GET /transfers/{transferId}` — confirmation & receipt payload (US-028)
- [ ] `GET /transfers` — the caller's own transfer history (US-028)

### Beneficiaries (US-029, US-030)

- [ ] `POST /beneficiaries` — add (US-029)
- [ ] `GET /beneficiaries` — list (US-029)
- [ ] `PATCH /beneficiaries/{id}` — edit (US-030)
- [ ] `DELETE /beneficiaries/{id}` — delete (US-030)

US-031 (favorites / quick transfer) is `[Flutter]`-only and ships in Sprint 5. If a `favorite` flag
on the beneficiary row is the cheapest way to support it, add the column here — but no screen.

### QR payments (US-032–US-034)

- [ ] `GET /qr/me` — generate the caller's personal QR payload (US-032)
- [ ] `POST /qr/pay` — resolve a scanned payload and settle it (US-033)
- [ ] Merchant payment path + seeded demo merchants, one always declining (US-034) — blocked on the
      spec above

**Keep the QR payload format simple** — the Sprint 5 mobile half should be genuinely just a camera
plus a POST. The scanning risk moved to Sprint 5; the payload work is here.

### Admin (US-050)

Admin-only and role-guarded — `@PreAuthorize` on `ADMIN`, verified by a test that a `CUSTOMER` token
gets `403`, same as every Sprint 3 admin endpoint.

- [ ] `GET /admin/transfers` — filterable transfer feed: date range, amount, status, account (US-050)

### Notifications (US-035)

- [ ] Transfer notifications hooked to the transfer service (US-035) — attaches to the existing
      `NotificationService` / `NotificationSender` seam built in Sprint 3, does **not** invent its
      own path

## Web admin pages (`web-admin/`)

- [ ] Transaction monitor — screen 4, read-only filterable transfer feed (US-050)
- [ ] Unstub US-053's transfer count & volume tiles on the KPI home, now that `transfers` exists —
      `AdminKpiServiceImpl` has the stub marked with a comment pointing at this sprint

`components/ui/data-table.tsx` is already generic (`DataTable<T>` with a `Column<T>` list) and screen
4 should reuse it rather than grow its own table. **There is no shared filter component yet** —
CustomersPage has a one-off search box. Screen 4 needs a real filter bar (date range, amount, status,
account); extract it as a reusable component here, because US-056's audit log viewer in Sprint 6 needs
the same thing and is supposed to be assembly, not a rebuild.

## Known gaps this sprint should close or carry

- **The access token still expires after 15 minutes with nothing refreshing it mid-session.**
  `auth-context` exchanges the refresh token only on page load, and neither `features/admin/api.ts`
  nor `features/customers/api.ts` has a 401-retry path. Sprint 3 deferred this to "US-046 or the H9
  revocation work" — US-046 shipped and it is still open. Screen 4 is another long-lived data screen
  that will hit the same wall, so fix it in the shared API helper before building it.
- **US-020 (statement email) is still homeless.** It was left open at the end of Sprint 2, blocked on
  the email-address source decision, and the Rev 3 resequencing did not assign it to a sprint. Either
  claim it here (Mailpit already exists, so it is small) or explicitly drop it from scope — leaving it
  unassigned is how it disappears.

## Out of scope for Sprint 4

All mobile work, including the money-movement screens for the APIs built here and US-031 favorites
(Sprint 5); bill payment and card management (Sprint 5); US-036 change password and US-037 forgot
password (Sprint 5); US-052 bill provider CRUD (Sprint 5); audit logs, hardening and SIT/UAT
(Sprint 6) — see sprint-plan.md.
