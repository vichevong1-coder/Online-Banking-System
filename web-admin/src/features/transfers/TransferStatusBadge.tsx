import { Badge } from "@/components/ui/badge"
import type { TransferStatus } from "@/features/transfers/api"

export function TransferStatusBadge({ status }: { status: TransferStatus | string }) {
  const normalized = (status || "").toUpperCase()
  if (normalized === "COMPLETED") {
    return (
      <Badge
        variant="secondary"
        className="border-emerald-200 bg-emerald-500/10 text-emerald-700 hover:bg-emerald-500/15 dark:border-emerald-800 dark:text-emerald-400"
      >
        completed
      </Badge>
    )
  }
  if (normalized === "FAILED") {
    return <Badge variant="destructive">failed</Badge>
  }
  if (normalized === "PENDING") {
    return (
      <Badge
        variant="outline"
        className="border-amber-300 bg-amber-500/10 text-amber-700 hover:bg-amber-500/15 dark:border-amber-800 dark:text-amber-400"
      >
        pending
      </Badge>
    )
  }
  return <Badge variant="outline">{status.toLowerCase()}</Badge>
}
