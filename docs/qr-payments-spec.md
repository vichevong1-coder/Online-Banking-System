# QR Payments Spec — US-032, US-033, US-034

The spec `sprint-plan.md` lists under "Still to be written" as the **US-034 merchant QR spec**. It
covers all three QR stories rather than US-034 alone, because the payload format is shared and
deciding it once for personal and merchant codes together is the only way it stays as simple as the
plan requires.

Backend only. The Flutter half of US-032/US-033 lands in Sprint 5; nothing here describes a screen.

## The constraint this spec is written against

From `sprint-plan.md`:

> Keep the payload format simple enough that the mobile half is genuinely just a camera plus a POST.

That is the governing rule. Every decision below that looks under-engineered — no signature, no
expiry, no encryption — is that rule being applied on purpose. The scanning work is the Sprint 5
schedule risk; the backend's job is to make sure the mobile side never has to do anything with the
scanned string except send it.

## Scope

| Story | Endpoint | What it does |
|---|---|---|
| US-032 | `GET /qr/me` | Returns the payload the caller's account is represented by |
| US-033 | `POST /qr/pay` | Resolves a scanned payload and settles it |
| US-034 | (same endpoint) | Merchant destinations, seeded, one always declining |

US-033 and US-034 are **one endpoint, not two**. A merchant payment differs from a person-to-person
payment only in what the payload resolves to, and splitting them would mean the mobile app has to
parse the payload to know where to POST it — exactly the thing the simplicity rule forbids.

## Payload format

A short delimited ASCII string. Not JSON, not base64, not a URL.

```
OBS1:<type>:<target>[:<currency>:<amount>]
```

| Field | Values |
|---|---|
| `OBS1` | Format marker and version. A payload not starting with it is rejected unparsed. |
| `type` | `P` — personal, `target` is an account number · `M` — merchant, `target` is a merchant code |
| `target` | The account number (12 digits, as seeded) or merchant code |
| `currency` | Optional, `USD` or `KHR`. Present only with `amount`. |
| `amount` | Optional, decimal with up to 4 places, matching `NUMERIC(19,4)` |

Examples:

```
OBS1:P:900000000001              a person, payer chooses the amount
OBS1:M:MERCH-ANGKOR:USD:12.50    a merchant asking for exactly $12.50
```

### Amount rules

- **Payload carries an amount** — it is fixed. The request may omit `amount`, or send one that
  matches exactly; anything else is `400 QR_AMOUNT_MISMATCH`. This is what stops a merchant's
  displayed price being quietly underpaid.
- **Payload carries no amount** — the request must supply `amount` and `currency`, or it is
  `400 VALIDATION_FAILED`.

### What the payload deliberately does not have

- **No signature or MAC.** It would need a key on every device and buys nothing: the payload names
  only a destination, and an account number is already what you hand someone to get paid. Every
  authorisation that matters happens server-side — the payer is the JWT, and the destination is
  re-resolved and re-validated on every `POST /qr/pay`.
- **No expiry and no nonce.** A personal QR is a stable address, like an account number. Replay is
  not a threat here because scanning a payload twice means the payer's own app deliberately paid
  twice, each time with their own token and each time debiting them.
- **No encryption.** There is nothing secret in it.

The residual risk, stated plainly: a substituted QR sticker sends money to the wrong destination.
The mitigation is a confirmation screen showing the resolved payee name and amount before the POST,
which is **Sprint 5's obligation and must not be dropped** — that is why `POST /qr/pay` is a
settle-now call and `GET /qr/resolve` is listed as deferred below rather than never.

## `GET /qr/me` (US-032)

Query: `accountId` (UUID, required) — which of the caller's accounts should be paid.

Owner-scoped like everything else: an account the caller does not own is
`404 ACCOUNT_NOT_FOUND`, never `403`.

```json
{
  "accountId": "a0000000-0000-0000-0000-000000000001",
  "accountNumber": "900000000001",
  "currency": "USD",
  "payload": "OBS1:P:900000000001"
}
```

The server returns the **string, not an image**. Rendering it as a QR bitmap is the mobile app's job
in Sprint 5 — a Dart QR widget is a few lines, whereas returning a PNG would put image encoding,
sizing and caching in the API for no gain.

`GET /qr/me` never carries an amount. A request-for-a-specific-amount code is a Sprint 5 idea at
best; the grammar already supports it, so adding it later needs no format change.

## Merchants (US-034)

A new table. Merchants are **not users** — they never log in, and giving them user rows would drag
in KYC, passwords and the whole auth surface for what the demo needs to be a payee.

```
merchants (
  id UUID PK,
  merchant_code VARCHAR(32) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  settlement_account_id UUID NOT NULL REFERENCES accounts(id),
  status VARCHAR(20) NOT NULL,     -- ACTIVE | ALWAYS_DECLINES
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

`settlement_account_id` is a real account row, so a merchant payment is an ordinary internal
transfer with a merchant on one end. No second ledger, no special-casing in the balance code.

`status = ALWAYS_DECLINES` is the failure path, expressed as data rather than a hardcoded
merchant code in the service. The declining merchant is a seeded row like the others, and a
reviewer can create a second one without touching Java.

### Seeded demo merchants

Three, per the tracker's minimum. They belong in `seed_demo_data.sql`, not in a Flyway migration —
same reason as every other demo row: migrations run in tests and CI, and the seed script does not.
They need a demo user to own the settlement accounts; use the existing `d0000000-` demo range and a
matching `a0000000-` account per merchant so the re-runnable delete block keeps working.

| Code | Name | Currency | Status |
|---|---|---|---|
| `MERCH-ANGKOR` | Angkor Coffee | USD | `ACTIVE` |
| `MERCH-PSAR` | Psar Thmei Market | KHR | `ACTIVE` |
| `MERCH-DECLINE` | Riverside Books | USD | `ALWAYS_DECLINES` |

The declining merchant is named like a normal shop on purpose — a demo that shows a decline from a
merchant called "Decline Test" demonstrates nothing.

## `POST /qr/pay` (US-033, US-034)

```json
{
  "payload": "OBS1:M:MERCH-ANGKOR:USD:12.50",
  "fromAccountId": "a0000000-0000-0000-0000-000000000001",
  "amount": "12.50",
  "currency": "USD",
  "description": "Coffee"
}
```

`amount`, `currency` and `description` are optional per the amount rules above. Returns `201` with
the **existing `TransferResponse`** — a QR payment is a transfer, and giving it its own response
shape would mean US-028's receipt and US-050's feed each need a second one.

### Settlement

One `@Transactional` method, the same shape as US-025:

1. Parse the payload. Unparseable, wrong marker, or unknown type → `400 QR_PAYLOAD_INVALID`.
2. Resolve `target` to a destination account — an account number for `P`, a merchant's settlement
   account for `M`. Unknown → `400 QR_TARGET_NOT_FOUND`.
3. Load the source account owner-scoped → `404 ACCOUNT_NOT_FOUND` if it is not the caller's.
4. Reconcile the amount against the payload → `400 QR_AMOUNT_MISMATCH`.
5. **If the merchant is `ALWAYS_DECLINES`, stop here** — see below.
6. Apply every US-027 rule unchanged: currency match, per-transfer cap, daily cap, sufficient funds.
7. Write the transfer and both ledger legs, debit and credit, mark `COMPLETED`, notify via US-035.

Settlement is instant, per the tracker. There is no `PENDING` state for QR — that exists only for
interbank (US-026).

### The decline path

A payment to an `ALWAYS_DECLINES` merchant:

- **writes a `FAILED` transfer row**, so the decline is visible in US-050's feed and in the payer's
  US-028 history. A decline that leaves no trace is not demoable.
- **moves no money.** No ledger legs, no debit. Checked before the balance is touched.
- returns **`400 MERCHANT_DECLINED`**.

`400` rather than `402 Payment Required`, for consistency: every business rejection in the transfer
slice — over-limit, insufficient funds, currency mismatch — is already a `400` with a coded body,
and one endpoint inventing a different status for the same class of outcome is worse than a slightly
loose status name.

**US-053's volume KPI must count `COMPLETED` only**, or these `FAILED` rows inflate it. The
declining merchant exists to be paid repeatedly in demos, so this will be visible if it is wrong.

### Rejections beyond the shared rules

- **Paying your own account** — `400 SAME_ACCOUNT_TRANSFER`, reusing US-025's exception. Scanning
  your own code is a mis-scan.
- **Cross-currency** — `400 CURRENCY_MISMATCH`, per the standing decision. A USD account cannot pay
  `MERCH-PSAR`. Worth demoing, since it is the one rejection a reviewer might mistake for a bug.
- **Person-to-person is not restricted to your own accounts.** This is the first endpoint where the
  destination is deliberately someone else's account — that is the entire point of a personal QR,
  and it is why `to_account_id` was never constrained to the caller.

## Limits

QR payments are transfers and share the US-027 daily cap with US-025 and US-026. They are not a
side channel around it. No separate QR cap: a second set of numbers to reason about, for a path that
is already capped, is complexity without a safety gain.

## Test checklist

The story is not done until these pass, in the style of `TransferControllerTest`:

- `GET /qr/me` returns a parseable payload; another customer's `accountId` is a `404`.
- Personal payment settles: both legs written, both balances moved, one `COMPLETED` transfer.
- Merchant payment settles into the merchant's settlement account.
- **The declining merchant returns `400 MERCHANT_DECLINED`, writes a `FAILED` transfer, and leaves
  the payer's balance byte-identical.**
- Payload with a fixed amount rejects a differing `amount`, and accepts an omitted one.
- Malformed payloads, unknown account numbers and unknown merchant codes are each rejected.
- Cross-currency, over-cap, and insufficient-funds rejections still fire on this path.
- QR payments count toward the same daily cap as US-025/US-026 transfers.
- Paying your own QR is rejected.
- All endpoints `403` unauthenticated.

## Deferred, with reasons

- **`GET /qr/resolve`** — a look-before-you-pay call returning the payee name and amount without
  settling. Sprint 5's confirmation screen is the natural consumer and should drive its shape, so it
  is better specified alongside that screen than guessed at now.
- **Merchant CRUD / a merchant admin screen.** Not in Sprint 4's scope, and seeded rows are enough
  for the demo. US-052's bill-provider CRUD in Sprint 5 is the same shape and is where a reusable
  answer should come from.
- **Dynamic per-transaction merchant codes** (a fresh code per till, per sale). Real merchant
  acquiring works this way; the demo does not need it, and static codes keep the payload static.
