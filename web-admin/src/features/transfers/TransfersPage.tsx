import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, RefreshCw, Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { DataTable, type Column } from "@/components/ui/data-table"
import { FilterBar, FilterItem, FilterSelect } from "@/components/ui/filter-bar"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/features/auth/auth-context"
import {
  formatDateTime,
  formatMoney,
  getTransfers,
  getTransferType,
  type TransferItem,
} from "@/features/transfers/api"
import { TransferStatusBadge } from "@/features/transfers/TransferStatusBadge"

const PAGE_SIZE = 20

// Screen 4 from the sprint plan (US-050) — read-only filterable transfer monitor feed.
export function TransfersPage() {
  const { accessToken } = useAuth()

  // Filter input states
  const [account, setAccount] = useState("")
  const [debouncedAccount, setDebouncedAccount] = useState("")
  const [status, setStatus] = useState<string>("ALL")
  const [currency, setCurrency] = useState<string>("ALL")
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [minAmount, setMinAmount] = useState("")
  const [debouncedMinAmount, setDebouncedMinAmount] = useState("")
  const [maxAmount, setMaxAmount] = useState("")
  const [debouncedMaxAmount, setDebouncedMaxAmount] = useState("")

  const [page, setPage] = useState(0)
  const [transfers, setTransfers] = useState<TransferItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Debounce text and amount inputs
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedAccount(account)
      setDebouncedMinAmount(minAmount)
      setDebouncedMaxAmount(maxAmount)
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [account, minAmount, maxAmount])

  const isFiltered = Boolean(
    account ||
      (status && status !== "ALL") ||
      (currency && currency !== "ALL") ||
      startDate ||
      endDate ||
      minAmount ||
      maxAmount,
  )

  const handleResetFilters = useCallback(() => {
    setAccount("")
    setDebouncedAccount("")
    setStatus("ALL")
    setCurrency("ALL")
    setStartDate("")
    setEndDate("")
    setMinAmount("")
    setDebouncedMinAmount("")
    setMaxAmount("")
    setDebouncedMaxAmount("")
    setPage(0)
  }, [])

  const fetchTransfers = useCallback(() => {
    if (!accessToken) return
    let cancelled = false

    setLoading(true)
    setError(null)
    getTransfers(accessToken, {
      page,
      size: PAGE_SIZE,
      account: debouncedAccount || undefined,
      status: status !== "ALL" ? status : undefined,
      currency: currency !== "ALL" ? currency : undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      minAmount: debouncedMinAmount || undefined,
      maxAmount: debouncedMaxAmount || undefined,
    })
      .then((result) => {
        if (cancelled) return
        setTransfers(result.content)
        setTotalElements(result.totalElements)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load transfers feed. Please try again.")
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [
    accessToken,
    page,
    debouncedAccount,
    status,
    currency,
    startDate,
    endDate,
    debouncedMinAmount,
    debouncedMaxAmount,
  ])

  useEffect(() => {
    return fetchTransfers()
  }, [fetchTransfers])

  const columns: Column<TransferItem>[] = [
    {
      key: "reference",
      header: "Reference",
      render: (transfer) => (
        <div className="flex flex-col">
          <span className="font-mono text-xs font-medium">{transfer.reference}</span>
          {transfer.externalRef && (
            <span className="max-w-[180px] truncate font-mono text-[11px] text-muted-foreground" title={transfer.externalRef}>
              {transfer.externalRef}
            </span>
          )}
          {transfer.description && (
            <span className="max-w-[180px] truncate text-[11px] text-muted-foreground" title={transfer.description}>
              {transfer.description}
            </span>
          )}
        </div>
      ),
    },
    {
      key: "type",
      header: "Type",
      render: (transfer) => (
        <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
          {getTransferType(transfer)}
        </span>
      ),
    },
    {
      key: "fromAccount",
      header: "From Account",
      render: (transfer) => <span className="font-mono text-xs">{transfer.fromAccountNumber}</span>,
    },
    {
      key: "toAccount",
      header: "To Account / Counterparty",
      render: (transfer) => (
        <span className="font-mono text-xs">
          {transfer.toAccountNumber || transfer.externalRef || "—"}
        </span>
      ),
    },
    {
      key: "amount",
      header: "Amount",
      render: (transfer) => (
        <span className="font-medium tabular-nums">
          {formatMoney(transfer.amount, transfer.currency)}
        </span>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (transfer) => <TransferStatusBadge status={transfer.status} />,
    },
    {
      key: "createdAt",
      header: "Date & Time",
      render: (transfer) => (
        <span className="text-xs text-muted-foreground whitespace-nowrap">
          {formatDateTime(transfer.createdAt)}
        </span>
      ),
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      {/* Header & title */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">Transfers Monitor</h2>
          <p className="text-sm text-muted-foreground">
            {loading ? "Loading…" : `${totalElements.toLocaleString()} transfer${totalElements === 1 ? "" : "s"} recorded`}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={fetchTransfers} disabled={loading}>
          <RefreshCw className={`mr-1 size-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Filter Bar */}
      <FilterBar onReset={handleResetFilters} isFiltered={isFiltered}>
        {/* Account search */}
        <FilterItem label="Account">
          <div className="relative w-44">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={account}
              onChange={(e) => setAccount(e.target.value)}
              placeholder="Account #"
              className="h-8 pl-8 text-xs"
              aria-label="Filter by account"
            />
          </div>
        </FilterItem>

        {/* Status */}
        <FilterItem label="Status">
          <FilterSelect
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
            aria-label="Filter by status"
          >
            <option value="ALL">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="PENDING">Pending</option>
          </FilterSelect>
        </FilterItem>

        {/* Currency */}
        <FilterItem label="Currency">
          <FilterSelect
            value={currency}
            onChange={(e) => {
              setCurrency(e.target.value)
              setPage(0)
            }}
            aria-label="Filter by currency"
            className="min-w-24"
          >
            <option value="ALL">All</option>
            <option value="USD">USD</option>
            <option value="KHR">KHR</option>
          </FilterSelect>
        </FilterItem>

        {/* Amount range */}
        <FilterItem label="Min Amount">
          <Input
            type="number"
            value={minAmount}
            onChange={(e) => setMinAmount(e.target.value)}
            placeholder="0.00"
            className="h-8 w-24 text-xs"
            aria-label="Filter by min amount"
            min="0"
            step="any"
          />
        </FilterItem>
        <FilterItem label="Max Amount">
          <Input
            type="number"
            value={maxAmount}
            onChange={(e) => setMaxAmount(e.target.value)}
            placeholder="0.00"
            className="h-8 w-24 text-xs"
            aria-label="Filter by max amount"
            min="0"
            step="any"
          />
        </FilterItem>

        {/* Date range */}
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
          <Button variant="outline" size="sm" onClick={fetchTransfers} className="border-destructive/40 hover:bg-destructive/10">
            Retry
          </Button>
        </div>
      )}

      {/* Data Table */}
      {!error && (
        <DataTable
          columns={columns}
          rows={transfers}
          getRowId={(transfer) => transfer.id}
          loading={loading}
          emptyMessage={isFiltered ? "No transfers match the filter criteria." : "No transfers recorded yet."}
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
