# Sprint 2 To-Do List — Accounts & Balances

Weeks 3–4. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

---

## Open decisions before this sprint starts

Called out in sprint-plan.md's "Still to be written" — resolve before the relevant story starts, not mid-implementation:

- **Notifications architecture note** — one `NotificationService` with pluggable channels (push, email, SMS), shared by US-022, US-023, and later US-035/US-060. Write the note before US-022 starts so US-023 doesn't invent its own path.
- **US-020 email-address source** — statement email has no KYC-collected address to send to, since registration (US-007) dropped the email field for customers. Decide: collect email as an optional profile field, or drop email delivery from US-020's scope.

## Backend design decisions (US-013–US-019)

- **Multi-currency (US-016)**: one account = one currency. A customer goes multi-currency by holding several accounts (e.g. a USD savings account and a KHR savings account), not via a single account with per-currency sub-balances. `Account.currency` is `USD` or `KHR`.
- **Account requests (US-014)**: auto-approved. `POST /accounts/requests` creates an `ACTIVE` account with a zero balance immediately — there's no admin approval screen for this anywhere in the Sprint 5 scope, so a pending-approval state would have no way to be resolved until a later sprint added one.
- Every `/accounts/**` endpoint is scoped to the authenticated caller's own accounts (ownership checked via `findByIdAndUserId`); a request for another customer's account returns `404 ACCOUNT_NOT_FOUND` rather than `403`, so account IDs can't be used to probe ownership.
- Transaction history filtering (US-017/US-018) goes through `JpaSpecificationExecutor`, not a `@Query` with `(:param IS NULL OR ...)` branches — the latter fails against Postgres because the driver can't infer a bind parameter's type when its only occurrence is inside an `IS NULL` check.
- Date filters (`fromDate`/`toDate` on transactions and statements) bucket by the **UTC** calendar day, not Cambodia local time (UTC+7). For a local demo this is a defensible default, but it means a transaction between 00:00–07:00 Cambodia time shows up under the previous day's bucket. Worth revisiting if Sprint 3 (transfers, which is where real transaction volume starts) needs local-day accuracy.

## API endpoints (`backend/`)

- [x] `GET /accounts` — view linked accounts (US-013)
- [x] `POST /accounts/requests` — open new account request (US-014)
- [x] `GET /accounts/{accountId}/balance` — real-time balance (US-015)
- [x] Multi-currency balance support — one account per currency, see decision above (US-016)
- [x] `GET /accounts/{accountId}/transactions` — transaction history, filter by date / type / amount (US-017, US-018)
- [x] `GET /accounts/{accountId}/statement` — generate PDF statement, via PDFBox (US-019)
- [ ] `POST /accounts/{accountId}/statement/email` — email statement via Mailpit (US-020) — blocked on the email-address source decision above
- [ ] `NotificationService` — pluggable channel infrastructure + device token registration endpoint (US-022)
- [ ] Balance-change alerts wired to `NotificationService` (US-023)

Backend note: the transactions table has no way to be populated yet — there's no deposit/withdrawal endpoint in Sprint 2, and fund transfers (which will write `TRANSFER_IN`/`TRANSFER_OUT` rows) don't land until Sprint 3. `GET /accounts/{accountId}/transactions` and the statement PDF are correct against an empty ledger today; they'll have real data to show once Sprint 3 ships. Tests cover filtering by seeding rows directly via SQL, the same pattern `AuthenticationControllerTest` uses for states with no service-level path to reach them.

## Mobile pages (`mobile/`)

- [x] Account list screen — linked accounts (US-013)
- [x] Open new account request form (US-014)
- [x] Balance display — real-time, with multi-currency selector (US-015, US-016)
- [x] Transaction history screen — list + filter by date / type / amount (US-017, US-018)
- [x] Statement in-app viewer — renders the PDF from the US-019 endpoint (US-021)
- [ ] Push notification setup — permission prompt, device token registration (US-022) — blocked, see note below
- [ ] Balance-change alert UI (US-023) — blocked, see note below
- [x] Account dashboard / home screen — aggregates accounts, balances, recent transactions (US-024)

US-022/US-023 are blocked on the backend: the "Notifications architecture note" under "Open decisions
before this sprint starts" above hasn't been written, `NotificationService` doesn't exist yet, and
there's no device-token registration endpoint to build a mobile client against. Building the mobile
UI now would mean inventing the exact contract that note is supposed to define.

## Out of scope for Sprint 2

Fund transfers, beneficiaries, QR payments (Sprint 3); bill payment, card management, password reset (Sprint 4); admin portal account/transfer/dashboard screens (Sprint 5) — see sprint-plan.md.
</content>
</invoke>
