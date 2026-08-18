# Sprint 3 To-Do List — Admin Portal I

Weeks 5–6. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

> **Resequenced (Rev 3).** Sprint 3 used to be Transfers & Payments; that work moved to Sprint 4
> (backend) and Sprint 5 (mobile). This sprint builds the staff portal instead, because Sprint 2
> already shipped the account backend it reads. See "Where the project actually stands" in
> sprint-plan.md.

---

## Before you start

- **Run the demo seed.** Every screen in this sprint renders customer/account/transaction data, and
  the database has none of its own after a rebuild:
  ```bash
  docker exec -i obs-postgres psql -U obs -d obs \
      < backend/src/main/resources/db/demo/seed_demo_data.sql
  ```
  Credentials for the seeded accounts are in `.claude/demo-admin.md` and `.claude/demo-customers.md`.
- **Decide the push channel for US-022.** Still unresolved in sprint-plan.md's "Still to be written"
  — there is no Firebase project for a local demo. Recommended default: a `notifications` table plus
  in-app polling, with FCM as a later swap behind the same interface. **US-022 and US-023 cannot
  start until this is decided.**
- **Name the display currency for US-053's volume KPI.** Accounts are USD and KHR, there is no FX
  rate table, and cross-currency conversion is explicitly out of scope — so the KPI has to declare
  which currency it reports in.

## Backend prerequisites

Neither exists today; both block a screen in this sprint.

- [ ] `login_attempts` table + record every failed login — US-053's failed-login tile has no data
      source without it, and US-058's lockout policy (Sprint 6) reuses the same counter
- [x] Account status **mutation** — `PATCH /admin/customers/{userId}/status`; `AccountStatusPolicy` still owns the login-side rule (US-048)

## API endpoints (`backend/`)

All of these are admin-only and must be role-guarded — `@PreAuthorize` on `ADMIN`, verified by a test
that a `CUSTOMER` token gets `403`.

- [ ] `POST /admin/me/password` — change own password (US-046)
- [x] `GET /admin/customers` — searchable, paginated customer list (US-054)
- [x] `GET /admin/customers/{userId}` — customer detail (US-054)
- [x] `GET /admin/customers/{userId}/accounts` — that customer's accounts + balances (US-049)
- [x] `PATCH /admin/customers/{userId}/status` — suspend / lock / reactivate (US-048)
- [ ] `GET /admin/staff` — list staff accounts (US-047)
- [ ] `POST /admin/staff` — create a staff account (US-047) — replaces the bootstrap-admin seed as the way staff come into existence
- [ ] `PATCH /admin/staff/{userId}/role` — assign one `Role` value (US-047)
- [ ] `GET /admin/kpis` — total customers, total accounts, failed-login count, today's transfer count & volume (US-053)

## Web admin pages (`web-admin/`)

Screens 0 (app shell) and 1 (login + 2FA) already exist from Sprint 1.

- [ ] KPI home — screen 2 (US-053)
- [x] Customers table with search — screen 3 (US-054)
- [x] Customer detail drawer: accounts + balances — screen 3 (US-049)
- [x] Suspend / lock / reactivate actions in the drawer — screen 3 (US-048)
- [ ] Roles screen: staff list, create staff, assign role — screen 5 (US-047)
- [ ] Admin settings: change own password (US-046)

**Build screens 2 and 3 with reusable table and filter components.** US-050 (Sprint 4), US-052
(Sprint 5) and US-056 (Sprint 6) are then assembly rather than a rebuild — this is the single
highest-leverage decision in this sprint.

## Notifications (`backend/`)

- [ ] `NotificationService` with pluggable channels (US-022) — blocked on the channel decision above
- [ ] `notifications` table (US-022)
- [ ] Balance-change alerts hooked to account balance mutations (US-023)

US-035 (transfer notifications, Sprint 4) and US-060 (live provider, Sprint 6) both attach to this
service rather than inventing their own path — build the seam now.

## Known gaps this sprint does not close

- **The access token expires after 15 minutes and nothing refreshes it mid-session.** `auth-context`
  exchanges the refresh token only on page load, and the admin API helper has no 401-retry path — so
  a dashboard left open past the access-token TTL starts showing "Couldn't load customers." rather
  than re-authenticating. Pre-existing plumbing gap that the first data screen is simply the first
  thing to expose. Fix it with US-046 or alongside the H9 revocation work in `architecture.md`; it is
  a shared-plumbing change, not something to bolt onto one screen.

- **US-053's transfer count & volume tiles have no data.** The `transfers` table does not exist until
  Sprint 4. Build the tiles stubbed and wire them in Sprint 4 — do not fake the numbers.
- **US-050 (transfer monitoring) and US-052 (bill providers) are not in this sprint.** Their data
  does not exist yet. The portal is *usable* at the end of this sprint, not *complete*.
- **The mobile half of US-022 / US-023** (in-app notification list) follows in Sprint 5 with the rest
  of the mobile work, per the one-sprint-behind rule. The backend seam ships here.

## Out of scope for Sprint 3

Transfers, beneficiaries and QR payments (Sprint 4 backend / Sprint 5 mobile); bill payment, cards
and password reset (Sprint 5); audit log viewer and hardening (Sprint 6) — see sprint-plan.md.
