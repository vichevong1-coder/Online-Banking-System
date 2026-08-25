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
  It now seeds customers, staff (ADMIN-role users), accounts, transactions, beneficiaries,
  transfers, the three QR merchants, bill providers, bill payments, recurring bills,
  notifications and cards — including a BLOCKED card for US-044 and a PIN-less one for US-045. Credentials are in `.claude/demo-admin.md` and `.claude/demo-customers.md`.
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

- [x] **A customer self-service surface.** US-036 needs it, US-020 depends on it existing, and
      US-045's card PIN will want it too. There is no customer-facing `/me` today. Build it once,
      under `feature/user/`, rather than three stories each bolting their own endpoint on.

## API endpoints (`backend/`)

Customer endpoints stay scoped to the caller's own resources, following the rule every sprint has
used: another customer's id returns `404`, never `403`, so ids can't probe ownership.

### Customer self-service & recovery (US-036, US-020, US-037)

- [x] `GET /me` — the caller's own profile
- [x] `PATCH /me` — update profile, including the optional email address (US-020's source)
- [x] `POST /me/password` — change password, current password required (US-036)
- [x] `POST /statements/{id}/email` — email a statement to the profile email (US-020), a clean
      coded 4xx when no address is set
- [x] `POST /auth/password/forgot` + `POST /auth/password/reset` — reset flow (US-037)
- [x] Reset UI on the admin login screen (US-037 `[React]`) — the shell exists from Sprint 1

### Bill payments (US-038, US-039, US-041, US-042)

- [x] Provider model + `V12` migration + seeded demo providers (US-038) — **blocks US-039**
- [x] `GET /bill-providers` — list payable providers (US-038)
- [x] `POST /bill-payments` — pay electricity / water / internet (US-039)
- [x] `GET /bill-payments` — payment history (US-042)
- [x] `GET /bill-payments/{id}` — one payment's receipt (US-042)
- [x] Scheduled / recurring payments (US-041) — **the designated drop, see above**

US-039 reuses US-027's limits and the US-035 notification through `TransferSupport`. It does not
get its own caps, its own notification path, or its own ledger.

### Card management (US-043, US-044, US-045)

- [x] Card model + migration (US-043)
- [x] `POST /cards` — request a new card (US-043)
- [x] `GET /cards` — the caller's own cards (US-043)
- [x] `POST /cards/{id}/block` + `/unblock` — (US-044)
- [x] `PATCH /cards/{id}` — set PIN and spending limits (US-045)

**Never store or return a PIN or a full PAN.** Hash the PIN like a password, and return a masked
number only. This is the one place in the project where getting it wrong is a real finding rather
than a style note, and Sprint 6's hardening pass will look straight at it.

### Admin (US-052)

- [x] `GET/POST/PATCH/DELETE /admin/bill-providers` — provider CRUD (US-052), `@PreAuthorize` on
      `ADMIN` and a test that a `CUSTOMER` token gets `403`, same as every admin endpoint since
      Sprint 3

## Web admin pages (`web-admin/`)

- [x] Bill provider CRUD screen (US-052) — the last portal screen except Sprint 6's audit log

Reuse what Sprint 4 left behind rather than rebuilding it: `components/ui/data-table.tsx` is generic
(`DataTable<T>` with a `Column<T>` list), `components/ui/filter-bar.tsx` was extracted for exactly
this kind of screen, and `lib/api-client.ts` already refreshes and replays on a 401. A new one-off
table or fetch helper here is a regression.

With US-052 the portal is complete except for the audit log (US-056, Sprint 6).

## Mobile (`mobile/`)

Ten stories against APIs that all already exist and are tested. That estimate was written when
`mobile/lib` was 20 Dart files across two features (`auth`, `account`) with a single test file; it
is now 52 files across nine features, with five test files.

- [x] Transfer between own accounts (US-025) — `internal_transfer_screen.dart`
- [x] Transfer to **another person's account at this bank** (US-026) — `p2p_transfer_screen.dart`.
      This half did not exist in Sprint 4: `POST /transfers` requires both accounts to be the
      caller's, so paying another customer was a 404. It is now `POST /transfers/p2p`, which
      resolves the destination by **account number** (a payer cannot know an account id) and reuses
      US-025's settlement, limits and ledger legs unchanged.
- [x] Transfer to another bank (US-026) — `interbank_transfer_screen.dart` against
      `POST /transfers/external`, with the seeded BIC codes; settles as `PENDING`
- [x] Confirmation & receipt (US-028) — `transfer_receipt_screen.dart`, driven by
      `transfer.status`: COMPLETED / PENDING / FAILED each render differently, so US-034's
      declining merchant cannot show up as a success
- [x] Add beneficiary (US-029) — from the manage screen and inline in the interbank flow
- [x] Edit / delete beneficiary (US-030)
- [x] Favorites / quick transfer (US-031) — favorites lead the quick-transfer strip and the manage
      list. **Called "Favorites" throughout the mobile UI**; "beneficiary" stays the backend's word
      for the row and never appears on a customer-facing screen
- [x] Generate personal QR (US-032) — `qr_scan_pay_screen.dart` renders `GET /qr/me`'s payload with
      `qr_flutter`, per receiving account
- [x] Scan & pay (US-033) — `qr_scanner_screen.dart` (`mobile_scanner`), pushed rather than embedded
      in the tab so the camera only runs while it is open. Payload parsed client-side
      (`qr_payload.dart`) to fill the confirmation sheet; a code that fixes its own amount locks the
      amount field
- [x] Transfer notifications (US-035) — the backend notifies on every transfer path; the app shows
      them in the list and on the home badge, polled every 30s and on app resume
- [x] In-app notification list & balance alerts (US-022 / US-023) — `notifications_screen.dart`,
      mark-as-read via `PATCH /notifications/{id}/read`

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

- [x] **`api_client.dart` 401 handling and token refresh** — closed. Every verb refreshes once and
      replays, and `/auth/login` and `/auth/refresh` are excluded so a bad password is not mistaken
      for an expired token.
- [x] **Mobile had one test file** — now five: widget, auth flow, dashboard navigation, QR payload
      parsing, and the transfer/beneficiary wire shapes.
- [x] **`actions/setup-java@v4`** — now `v5` in `backend-ci.yml`.
- [x] **Notification isolation** — closed by `TransferCompletedNotificationListener`, an
      `AFTER_COMMIT` + `REQUIRES_NEW` listener, so a notification failure can no longer mark a
      settled transfer's transaction rollback-only.

Closed during the sprint, worth recording because they were real defects rather than missing work:

- **The mobile client was calling endpoints that do not exist.** `POST /transfers/internal` and
  `/transfers/interbank` (the backend has `POST /transfers` and `/transfers/external`),
  `POST /notifications/{id}/read` (it is a `PATCH`), and a beneficiary shape built on
  `name`/`nickname`/`isInternal` where `BeneficiaryResponse` emits `displayName`/`favorite`.
- **Every request sent a lowercase currency** (`"usd"`), and `application.properties` does not set
  `accept-case-insensitive-enums` — so bill payments, recurring bills, QR pay and interbank were all
  failing validation on that field. All serialization now goes through `Currency.toJson()`.
- **US-024's session store did not match its acceptance criteria.** Both tokens were in plaintext
  `SharedPreferences`; the refresh token now lives in `flutter_secure_storage` and the access token
  is memory-only, exchanged on launch behind a splash state.
- **Dead and duplicate screens removed**: the Sprint-1 `registration_screen.dart` (superseded by the
  phone → details → OTP flow, its validation ported over), `qr_screen.dart` (a second copy of the
  Receive tab), and `account_list_screen.dart` / `account_detail_screen.dart` (reachable only from
  each other).

## Out of scope for Sprint 5

Mobile bills and cards, and mobile password recovery — they follow their backends into Sprint 6,
the same one-sprint-behind rule this plan has used throughout. Audit logging (US-056), hardening
(US-058, US-059), the live SMS provider swap (US-060), and SIT/UAT are all Sprint 6 — see
sprint-plan.md.
