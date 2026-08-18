import { useCallback, useEffect, useState } from "react"
import { Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { DataTable, type Column } from "@/components/ui/data-table"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/features/auth/auth-context"
import {
  formatDate,
  getCustomer,
  getCustomerAccounts,
  searchCustomers,
  type Account,
  type CustomerDetail,
  type CustomerSummary,
} from "@/features/customers/api"
import { CustomerDetailDrawer } from "@/features/customers/CustomerDetailDrawer"
import { StatusBadge } from "@/features/customers/StatusBadge"

const PAGE_SIZE = 20


// Screen 3 from the sprint plan's screen inventory — the customer table (US-054) plus the detail
// drawer showing that customer's accounts and balances (US-049). The suspend / lock / reactivate
// actions (US-048) attach to the drawer once the status-mutation endpoint exists.
export function CustomersPage() {
  const { accessToken } = useAuth()

  const [search, setSearch] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [page, setPage] = useState(0)

  const [customers, setCustomers] = useState<CustomerSummary[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [selected, setSelected] = useState<CustomerSummary | null>(null)
  const [detail, setDetail] = useState<CustomerDetail | null>(null)
  const [accounts, setAccounts] = useState<Account[] | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)

  // Debounce so typing a name doesn't fire a request per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search)
      setPage(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    if (!accessToken) return
    let cancelled = false

    setLoading(true)
    setError(null)
    searchCustomers(accessToken, { search: debouncedSearch, page, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return
        setCustomers(result.content)
        setTotalElements(result.totalElements)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load customers. Please try again.")
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    // Guard against an out-of-order response overwriting a newer one when the search changes fast.
    return () => {
      cancelled = true
    }
  }, [accessToken, debouncedSearch, page])

  const openCustomer = useCallback(
    (customer: CustomerSummary) => {
      if (!accessToken) return
      setSelected(customer)
      setDetail(null)
      setAccounts(null)
      setDetailError(null)

      Promise.all([getCustomer(accessToken, customer.id), getCustomerAccounts(accessToken, customer.id)])
        .then(([customerDetail, customerAccounts]) => {
          setDetail(customerDetail)
          setAccounts(customerAccounts)
        })
        .catch(() => setDetailError("Couldn't load this customer's details."))
    },
    [accessToken],
  )

  const columns: Column<CustomerSummary>[] = [
    {
      key: "name",
      header: "Name",
      render: (customer) => (
        <span className="font-medium">
          {customer.firstName} {customer.lastName}
        </span>
      ),
    },
    { key: "phone", header: "Phone", render: (customer) => customer.phone },
    { key: "status", header: "Status", render: (customer) => <StatusBadge status={customer.status} /> },
    {
      key: "phoneVerified",
      header: "Verified",
      render: (customer) => (
        <span className={customer.phoneVerified ? "text-muted-foreground" : "text-destructive"}>
          {customer.phoneVerified ? "Yes" : "No"}
        </span>
      ),
    },
    {
      key: "createdAt",
      header: "Registered",
      render: (customer) => <span className="text-muted-foreground">{formatDate(customer.createdAt)}</span>,
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">Customers</h2>
          <p className="text-sm text-muted-foreground">
            {loading ? "Loading…" : `${totalElements} customer${totalElements === 1 ? "" : "s"}`}
          </p>
        </div>
        <div className="relative w-full max-w-xs">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search name or phone"
            className="pl-8"
            aria-label="Search customers by name or phone"
          />
        </div>
      </div>

      {error ? (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      ) : (
        <DataTable
          columns={columns}
          rows={customers}
          getRowId={(customer) => customer.id}
          loading={loading}
          emptyMessage={debouncedSearch ? `No customers match "${debouncedSearch}".` : "No customers yet."}
          onRowClick={openCustomer}
          activeRowId={selected?.id ?? null}
        />
      )}

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

      <CustomerDetailDrawer
        summary={selected}
        detail={detail}
        accounts={accounts}
        error={detailError}
        onClose={() => setSelected(null)}
      />
    </div>
  )
}
