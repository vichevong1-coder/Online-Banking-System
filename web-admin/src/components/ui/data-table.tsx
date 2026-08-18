import type { ReactNode } from "react"

import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

// Shared table for the admin portal's list screens. Built here rather than inside the customers
// feature because the sprint plan's screen inventory has three more tables coming that are the same
// shape — the transfer monitor (US-050), bill providers (US-052) and the audit log (US-056).
export type Column<T> = {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** Extra classes for both the header cell and the body cells in this column. */
  className?: string
}

type DataTableProps<T> = {
  columns: Column<T>[]
  rows: T[]
  getRowId: (row: T) => string
  loading?: boolean
  /** Shown when there are no rows and we are not loading. */
  emptyMessage?: string
  onRowClick?: (row: T) => void
  /** Row highlighted as currently open, e.g. the row whose detail drawer is showing. */
  activeRowId?: string | null
}

export function DataTable<T>({
  columns,
  rows,
  getRowId,
  loading = false,
  emptyMessage = "Nothing to show.",
  onRowClick,
  activeRowId = null,
}: DataTableProps<T>) {
  const interactive = Boolean(onRowClick)

  return (
    // Tables are the one thing that reliably breaks narrow layouts; keep the overflow on the
    // wrapper so the page itself never scrolls sideways.
    <div className="w-full overflow-x-auto rounded-md border">
      <table className="w-full caption-bottom text-sm">
        <thead className="border-b bg-muted/50">
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={cn("px-4 py-2.5 text-left font-medium text-muted-foreground", column.className)}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading &&
            Array.from({ length: 5 }).map((_, rowIndex) => (
              <tr key={`skeleton-${rowIndex}`} className="border-b last:border-b-0">
                {columns.map((column) => (
                  <td key={column.key} className={cn("px-4 py-3", column.className)}>
                    <Skeleton className="h-4 w-24" />
                  </td>
                ))}
              </tr>
            ))}

          {!loading && rows.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="px-4 py-10 text-center text-muted-foreground">
                {emptyMessage}
              </td>
            </tr>
          )}

          {!loading &&
            rows.map((row) => {
              const id = getRowId(row)
              return (
                <tr
                  key={id}
                  onClick={onRowClick ? () => onRowClick(row) : undefined}
                  // Rows are clickable, so they must also be reachable and activatable by keyboard.
                  tabIndex={interactive ? 0 : undefined}
                  role={interactive ? "button" : undefined}
                  onKeyDown={
                    onRowClick
                      ? (event) => {
                          if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault()
                            onRowClick(row)
                          }
                        }
                      : undefined
                  }
                  className={cn(
                    "border-b last:border-b-0",
                    interactive && "cursor-pointer hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none",
                    activeRowId === id && "bg-muted",
                  )}
                >
                  {columns.map((column) => (
                    <td key={column.key} className={cn("px-4 py-3", column.className)}>
                      {column.render(row)}
                    </td>
                  ))}
                </tr>
              )
            })}
        </tbody>
      </table>
    </div>
  )
}
