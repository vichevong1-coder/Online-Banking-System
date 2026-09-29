import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, RefreshCw } from "lucide-react"

import { Button } from "@/components/ui/button"
import { DataTable, type Column } from "@/components/ui/data-table"
import { FilterBar, FilterItem } from "@/components/ui/filter-bar"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/features/auth/auth-context"
import { formatDateTime } from "@/features/transfers/api"
import { getAuditLogs, type AuditLog } from "@/features/audit-logs/api"

const PAGE_SIZE = 20

export function AuditLogsPage() {
  const { accessToken } = useAuth()

  // Filter input states
  const [actionType, setActionType] = useState<string>("ALL")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")

  const [page, setPage] = useState(0)
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const isFiltered = Boolean(
    (actionType && actionType !== "ALL") || startDate || endDate,
  )

  const handleResetFilters = useCallback(() => {
    setActionType("ALL")
    setStartDate("")
    setEndDate("")
    setPage(0)
  }, [])

  const fetchLogs = useCallback(() => {
    if (!accessToken) return
    let cancelled = false

    setLoading(true)
    setError(null)
    getAuditLogs(accessToken, {
      page,
      size: PAGE_SIZE,
      actionType: actionType !== "ALL" ? actionType : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    })
      .then((result) => {
        if (cancelled) return
        setLogs(result.content)
        setTotalElements(result.totalElements)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load audit logs. Please try again.")
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [accessToken, page, actionType, startDate, endDate])

  useEffect(() => {
    return fetchLogs()
  }, [fetchLogs])

  // date (createdAt), actor (actorEmail), action (actionType), details (details), ip (ipAddress)
  const columns: Column<AuditLog>[] = [
    {
      key: "createdAt",
      header: "Date & Time",
      render: (log) => (
        <span className="text-xs text-muted-foreground whitespace-nowrap">
          {formatDateTime(log.createdAt)}
        </span>
      ),
    },
    {
      key: "actorEmail",
      header: "Actor",
      render: (log) => <span className="text-xs">{log.actorEmail}</span>,
    },
    {
      key: "actionType",
      header: "Action",
      render: (log) => <span className="font-mono text-xs font-medium">{log.actionType}</span>,
    },
    {
      key: "details",
      header: "Details",
      render: (log) => (
        <span className="max-w-xs truncate text-[11px] text-muted-foreground" title={log.details ?? undefined}>
          {log.details || "—"}
        </span>
      ),
    },
    {
      key: "ipAddress",
      header: "IP Address",
      render: (log) => <span className="font-mono text-xs">{log.ipAddress || "—"}</span>,
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      {/* Header & title */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">Audit Logs</h2>
          <p className="text-sm text-muted-foreground">
            {loading ? "Loading…" : `${totalElements.toLocaleString()} log${totalElements === 1 ? "" : "s"} recorded`}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={fetchLogs} disabled={loading}>
          <RefreshCw className={`mr-1 size-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Filter Bar */}
      <FilterBar onReset={handleResetFilters} isFiltered={isFiltered}>
        <FilterItem label="Action Type">
          <Input
            value={actionType === "ALL" ? "" : actionType}
            onChange={(e) => {
              setActionType(e.target.value || "ALL")
              setPage(0)
            }}
            placeholder="Search action..."
            className="h-8 w-44 text-xs"
            aria-label="Filter by action type"
          />
        </FilterItem>

        <FilterItem label="From Date">
          <Input
            type="date"
            value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value)
              setPage(0)
            }}
            className="h-8 w-36 text-xs"
            aria-label="Filter by start date"
          />
        </FilterItem>
        <FilterItem label="To Date">
          <Input
            type="date"
            value={endDate}
            onChange={(e) => {
              setEndDate(e.target.value)
              setPage(0)
            }}
            className="h-8 w-36 text-xs"
            aria-label="Filter by end date"
          />
        </FilterItem>
      </FilterBar>

      {/* Error banner */}
      {error && (
        <div className="flex items-center justify-between rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <div className="flex items-center gap-2">
            <AlertTriangle className="size-4 shrink-0" />
            <span>{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={fetchLogs} className="border-destructive/40 hover:bg-destructive/10">
            Retry
          </Button>
        </div>
      )}

      {/* Data Table */}
      {!error && (
        <DataTable
          columns={columns}
          rows={logs}
          getRowId={(log) => log.id}
          loading={loading}
          emptyMessage={isFiltered ? "No audit logs match the filter criteria." : "No audit logs recorded yet."}
        />
      )}

      {/* Pagination controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
