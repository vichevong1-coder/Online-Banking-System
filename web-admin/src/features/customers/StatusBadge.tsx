import { Badge } from "@/components/ui/badge"
import type { AccountStatus } from "@/features/customers/api"

// Shared by the customer table and the detail drawer. US-048's suspend / lock / reactivate actions
// will drive the same three states, so the mapping lives in one place.
const STATUS_VARIANT: Record<AccountStatus, "default" | "secondary" | "destructive" | "outline"> = {
  ACTIVE: "secondary",
  SUSPENDED: "destructive",
  LOCKED: "outline",
}

export function StatusBadge({ status }: { status: AccountStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{status.toLowerCase()}</Badge>
}
