import { useState } from "react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import {
  formatDate,
  formatMoney,
  type Account,
  type AccountStatus,
  type CustomerDetail,
  type CustomerSummary,
} from "@/features/customers/api"
import { StatusBadge } from "@/features/customers/StatusBadge"


function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      {/* KYC fields are nullable on the entity, so an em dash is a real case, not a fallback. */}
      <dd className="text-sm">{value ?? "—"}</dd>
    </div>
  )
}

// US-048. Every transition is permitted, so the buttons offered are simply the two states the
// customer is not currently in.
const STATUS_ACTIONS: { status: AccountStatus; label: string; destructive: boolean }[] = [
  { status: "ACTIVE", label: "Reactivate", destructive: false },
  { status: "SUSPENDED", label: "Suspend", destructive: true },
  { status: "LOCKED", label: "Lock", destructive: true },
]

type Props = {
  summary: CustomerSummary | null
  detail: CustomerDetail | null
  accounts: Account[] | null
  error: string | null
  onClose: () => void
  onChangeStatus: (customerId: string, status: AccountStatus) => Promise<void>
}

// The detail half of screen 3: KYC data from US-054, accounts and balances from US-049.
// The summary is passed alongside the detail so the header renders immediately on row click while
// the two detail requests are still in flight.
export function CustomerDetailDrawer({ summary, detail, accounts, error, onClose, onChangeStatus }: Props) {
  const [pendingStatus, setPendingStatus] = useState<AccountStatus | null>(null)

  async function applyStatus(customerId: string, status: AccountStatus) {
    setPendingStatus(status)
    try {
      await onChangeStatus(customerId, status)
    } finally {
      setPendingStatus(null)
    }
  }

  return (
    <Sheet open={summary !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-md">
        {summary && (
          <>
            <SheetHeader>
              <SheetTitle>
                {summary.firstName} {summary.lastName}
              </SheetTitle>
              <SheetDescription>
                {summary.phone} · registered {formatDate(summary.createdAt)}
              </SheetDescription>
            </SheetHeader>

            <div className="flex flex-col gap-6 px-4 pb-6">
              <div className="flex flex-wrap items-center gap-2">
                <StatusBadge status={summary.status} />
                {!summary.phoneVerified && <Badge variant="outline">phone unverified</Badge>}
              </div>

              {error && <p className="text-sm text-destructive">{error}</p>}

              <section className="flex flex-col gap-2">
                <h3 className="text-sm font-medium">Account status</h3>
                <div className="flex flex-wrap gap-2">
                  {STATUS_ACTIONS.filter((action) => action.status !== summary.status).map((action) => (
                    <Button
                      key={action.status}
                      size="sm"
                      variant={action.destructive ? "destructive" : "default"}
                      disabled={pendingStatus !== null}
                      onClick={() => applyStatus(summary.id, action.status)}
                    >
                      {pendingStatus === action.status ? "Working…" : action.label}
                    </Button>
                  ))}
                </div>
                <p className="text-xs text-muted-foreground">
                  A suspended or locked customer cannot sign in. An active session ends within 15
                  minutes, when its access token expires.
                </p>
              </section>

              <section className="flex flex-col gap-3">
                <h3 className="text-sm font-medium">Identity (KYC)</h3>
                {!detail && !error ? (
                  <div className="flex flex-col gap-2">
                    <Skeleton className="h-4 w-40" />
                    <Skeleton className="h-4 w-32" />
                  </div>
                ) : (
                  detail && (
                    <dl className="grid grid-cols-2 gap-3">
                      <Field label="NID number" value={detail.nidNumber} />
                      <Field label="NID expiry" value={detail.nidExpiryDate ? formatDate(detail.nidExpiryDate) : null} />
                      <Field label="Date of birth" value={detail.dateOfBirth ? formatDate(detail.dateOfBirth) : null} />
                      <Field label="Gender" value={detail.gender?.toLowerCase()} />
                    </dl>
                  )
                )}
              </section>

              <Separator />

              <section className="flex flex-col gap-3">
                <h3 className="text-sm font-medium">Accounts</h3>
                {!accounts && !error ? (
                  <div className="flex flex-col gap-2">
                    <Skeleton className="h-12 w-full" />
                    <Skeleton className="h-12 w-full" />
                  </div>
                ) : accounts && accounts.length === 0 ? (
                  <p className="text-sm text-muted-foreground">This customer has no accounts yet.</p>
                ) : (
                  accounts && (
                    <ul className="flex flex-col gap-2">
                      {accounts.map((account) => (
                        <li
                          key={account.id}
                          className="flex items-center justify-between gap-3 rounded-md border px-3 py-2"
                        >
                          <div className="flex flex-col">
                            <span className="font-mono text-sm">{account.accountNumber}</span>
                            <span className="text-xs text-muted-foreground">
                              {account.accountType.toLowerCase()} · {account.currency}
                            </span>
                          </div>
                          <span className="text-sm font-medium tabular-nums">
                            {formatMoney(account.balance, account.currency)}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )
                )}
              </section>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}
