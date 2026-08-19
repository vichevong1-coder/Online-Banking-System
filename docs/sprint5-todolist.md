# Sprint 5 To-Do List — Bills & Cards (backend), Admin CRUD, Mobile Money Movement

Weeks 9–10. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

> **This is the heaviest sprint in the plan** — 11 backend/admin stories plus 10 mobile ones. It is
> heavy *by construction*: deferring mobile stacked it behind the backend rather than removing it.
> The mobile half is not a bonus at the end; it is half the sprint.

---

## Before you start

- **Run the demo seed.** The database has no data of its own after a rebuild:
  ```bash
  docker exec -i obs-postgres psql -U obs -d obs \
      < backend/src/main/resources/db/demo/seed_demo_data.sql
  ```
  It now seeds customers, accounts, transactions, beneficiaries, transfers and the three QR
  merchants. Credentials are in `.claude/demo-admin.md` and `.claude/demo-customers.md`.
- **Tests no longer use your local database.** As of Sprint 4 the backend suite starts a throwaway
  Postgres through Testcontainers (`backend/src/test/resources/application-test.properties`), so
  `./mvnw test` needs Docker running but does not care what is in `obs-postgres`. Do not write a
  test that assumes a globally empty table — that is exactly what broke when the seed grew.
- **The latest migration is `V11`.** Yours start at `V12`. Bill providers and cards each need one.
- **Seed bill providers as part of US-038**, in `seed_demo_data.sql` and not in a migration, and
  extend the re-runnable delete block at the top of that file the way the merchant rows did.
  Anything Flyway picks up also runs in tests and CI, where demo rows have no business.

## Decisions already made — do not relitigate mid-implementation

- **US-020's email address is an optional profile field, never a KYC field.** Registration
  deliberately dropped email in Sprint 1 and customers are identified by phone. That stays true.
- **US-020 is built after US-036, never before.** Customers have no self-service surface today —
  admins have `AdminSelfController`, customers have nothing — and US-036 is what creates one.
  Building US-020 first means inventing a customer profile API that US-036 would immediately rework.
- **Tripwire: if US-036 has not landed by the midpoint of this sprint, drop US-020 outright** — cut
  the story, remove Mailpit from `docker-compose.yml`, leave `spring.mail.*` unconfigured.
  Statements already download without it. It has been homeless since Sprint 2 and has survived one
  resequencing; a clean deletion is an acceptable outcome, a third deferral into Sprint 6 is not.
- **US-041 is the designated drop.** If something has to give, scheduled/recurring payments go
  first: it is the only story here with no demo dependency on anything else. Decide this early
  rather than by running out of time.
- **Bill payments and card transactions are transfers, not a parallel ledger.** They go through the
  `transfers` table and `TransferSupport`, the same as US-025/026 and QR. Anything that moves money
  and does not appear in US-050's admin feed is a bug.

## Backend prerequisites

- [ ] **A customer self-service surface.** US-036 needs it, US-020 depends on it existing, and
      US-045's card PIN will want it too. There is no customer-facing `/me` today. Build it once,
      under `feature/user/`, rather than three stories each bolting their own endpoint on.

## API endpoints (`backend/`)

Customer endpoints stay scoped to the caller's own resources, following the rule every sprint has
used: another customer's id returns `404`, never `403`, so ids can't probe ownership.

### Customer self-service & recovery (US-036, US-020, US-037)

- [ ] `GET /me` — the caller's own profile
- [ ] `PATCH /me` — update profile, including the optional email address (US-020's source)
- [ ] `POST /me/password` — change password, current password required (US-036)
- [ ] `POST /statements/{id}/email` — email a statement to the profile email (US-020), a clean
      coded 4xx when no address is set
- [ ] `POST /auth/password/forgot` + `POST /auth/password/reset` — reset flow (US-037)
- [ ] Reset UI on the admin login screen (US-037 `[React]`) — the shell exists from Sprint 1

### Bill payments (US-038, US-039, US-041, US-042)

- [ ] Provider model + `V12` migration + seeded demo providers (US-038) — **blocks US-039**
- [ ] `GET /bill-providers` — list payable providers (US-038)
- [ ] `POST /bill-payments` — pay electricity / water / internet (US-039)
- [ ] `GET /bill-payments` — payment history (US-042)
- [ ] `GET /bill-payments/{id}` — one payment's receipt (US-042)
- [ ] Scheduled / recurring payments (US-041) — **the designated drop, see above**

US-039 reuses US-027's limits and the US-035 notification through `TransferSupport`. It does not
get its own caps, its own notification path, or its own ledger.

### Card management (US-043, US-044, US-045)

- [ ] Card model + migration (US-043)
- [ ] `POST /cards` — request a new card (US-043)
- [ ] `GET /cards` — the caller's own cards (US-043)
- [ ] `POST /cards/{id}/block` + `/unblock` — (US-044)
- [ ] `PATCH /cards/{id}` — set PIN and spending limits (US-045)

**Never store or return a PIN or a full PAN.** Hash the PIN like a password, and return a masked
number only. This is the one place in the project where getting it wrong is a real finding rather
than a style note, and Sprint 6's hardening pass will look straight at it.

### Admin (US-052)

- [ ] `GET/POST/PATCH/DELETE /admin/bill-providers` — provider CRUD (US-052), `@PreAuthorize` on
      `ADMIN` and a test that a `CUSTOMER` token gets `403`, same as every admin endpoint since
      Sprint 3

## Web admin pages (`web-admin/`)

- [ ] Bill provider CRUD screen (US-052) — the last portal screen except Sprint 6's audit log

Reuse what Sprint 4 left behind rather than rebuilding it: `components/ui/data-table.tsx` is generic
(`DataTable<T>` with a `Column<T>` list), `components/ui/filter-bar.tsx` was extracted for exactly
this kind of screen, and `lib/api-client.ts` already refreshes and replays on a 401. A new one-off
table or fetch helper here is a regression.

With US-052 the portal is complete except for the audit log (US-056, Sprint 6).

## Mobile (`mobile/`)

Ten stories against APIs that all already exist and are tested. **Read the state of the app before
estimating**: `mobile/lib` is 20 Dart files across two features (`auth`, `account`), and
`mobile/test` contains a single test file.

- [ ] Transfer between own accounts (US-025)
- [ ] Transfer to another bank (US-026)
- [ ] Confirmation & receipt (US-028)
- [ ] Add beneficiary (US-029)
- [ ] Edit / delete beneficiary (US-030)
- [ ] Favorites / quick transfer (US-031) — the `favorite` flag already exists on the beneficiary
      row and is settable via `PATCH /beneficiaries/{id}`; no new endpoint is needed
- [ ] Generate personal QR (US-032) — `GET /qr/me` returns the payload **string**; rendering it as
      a QR bitmap is this story
- [ ] Scan & pay (US-033) — **start this first, see below**
- [ ] Transfer notifications (US-035)
- [ ] In-app notification list & balance alerts (US-022 / US-023) — backend shipped in Sprint 3

Notes that will decide whether this half lands:

- **US-033 is the schedule risk in the whole plan.** It needs a physical device and cannot be fully
  verified in an emulator. Start it in week 9, not week 10. The payload format was kept deliberately
  simple for this reason — see [`qr-payments-spec.md`](./qr-payments-spec.md); the mobile side is a
  camera plus a POST, and if it is turning into more than that, re-read the spec before writing code.
- **`pubspec.yaml` has no QR or camera package yet.** US-032 needs QR generation and US-033 needs
  scanning. Add them with `flutter pub add` rather than pinning versions by hand.
- **There is no state-management package** — the app is `http` plus widget state. Nine new screens
  is the point where that either holds or stops holding. Decide deliberately at the start of the
  sprint; converting halfway through is the expensive path.
- **A confirmation step before `POST /qr/pay` is not optional.** The QR spec accepts an unsigned
  payload specifically because the mobile app shows the resolved payee and amount before paying.
  That screen is this sprint's obligation.

## Known gaps this sprint should close or carry

- **`mobile/lib/core/api/api_client.dart` has no 401 handling and no token refresh.** It is the same
  gap web-admin carried from Sprint 3 and closed in Sprint 4 — and as of Sprint 4 the backend
  actually returns `401` for an expired token rather than `403`, so the behaviour mobile sees has
  changed. Every screen in this sprint is a long-lived data screen that will hit a 15-minute token
  expiry mid-session. Fix it in the shared client before building the screens, not after.
- **Mobile has one test file.** Ten stories are about to land on top of that. Even a thin widget
  test per flow would change what Sprint 6's SIT/UAT has to discover by hand.
- **`actions/setup-java@v4` is deprecated** in `backend-ci.yml` and warns on every run. One-line fix
  to `v5`; do it in passing.
- **Notification isolation is still partial.** `NotificationServiceImpl.sendNotification` is
  `@Transactional(REQUIRED)` and joins the caller's transaction, so a failure there can still mark
  a settled transfer's transaction rollback-only despite the catch in `TransferSupport`. Now that
  bill payments are about to become a third caller, either switch it to `REQUIRES_NEW` or move it
  behind an `AFTER_COMMIT` listener — the tests can be honest about it now that they run against a
  throwaway container.

## Out of scope for Sprint 5

Mobile bills and cards, and mobile password recovery — they follow their backends into Sprint 6,
the same one-sprint-behind rule this plan has used throughout. Audit logging (US-056), hardening
(US-058, US-059), the live SMS provider swap (US-060), and SIT/UAT are all Sprint 6 — see
sprint-plan.md.
