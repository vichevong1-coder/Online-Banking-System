import { useCallback, useEffect, useState } from "react"
import { AlertTriangle, ArrowLeftRight, DollarSign, Landmark, RefreshCw, ShieldAlert, Users } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuth } from "@/features/auth/auth-context"
import { formatMoney } from "@/features/customers/api"
import { getKpis, type KpiData } from "@/features/admin/api"

export function OverviewPage() {
  const { accessToken } = useAuth()
  const [kpis, setKpis] = useState<KpiData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchKpis = useCallback(async () => {
    if (!accessToken) return
    setLoading(true)
    setError(null)
    try {
      const data = await getKpis(accessToken)
      setKpis(data)
    } catch {
      setError("Couldn't load dashboard overview KPIs. Please try again.")
    } finally {
      setLoading(false)
    }
  }, [accessToken])

  useEffect(() => {
    fetchKpis()
  }, [fetchKpis])

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-heading text-lg font-semibold">Overview</h2>
          <p className="text-sm text-muted-foreground">High-level operations and system metrics</p>
        </div>
        <Button variant="outline" size="sm" onClick={fetchKpis} disabled={loading}>
          <RefreshCw className={`size-4 mr-1 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {error && (
        <div className="flex items-center justify-between rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <div className="flex items-center gap-2">
            <AlertTriangle className="size-4 shrink-0" />
            <span>{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={fetchKpis} className="border-destructive/40 hover:bg-destructive/10">
            Retry
          </Button>
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {/* Total Customers */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Customers</CardTitle>
            <Users className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <div className="text-2xl font-bold">{kpis?.totalCustomers.toLocaleString() ?? "—"}</div>
            )}
            <p className="mt-1 text-xs text-muted-foreground">Registered customer profiles</p>
          </CardContent>
        </Card>

        {/* Total Accounts */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Accounts</CardTitle>
            <Landmark className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <div className="text-2xl font-bold">{kpis?.totalAccounts.toLocaleString() ?? "—"}</div>
            )}
            <p className="mt-1 text-xs text-muted-foreground">Savings and checking accounts</p>
          </CardContent>
        </Card>

        {/* Failed Logins */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Failed Logins</CardTitle>
            <ShieldAlert className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <div className="text-2xl font-bold text-destructive">
                {kpis?.failedLogins.toLocaleString() ?? "—"}
              </div>
            )}
            <p className="mt-1 text-xs text-muted-foreground">Authentication failures recorded</p>
          </CardContent>
        </Card>

        {/* Today's Transfers */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Today's Transfers</CardTitle>
            <ArrowLeftRight className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <div className="flex items-baseline gap-2">
                <div className="text-2xl font-bold">{kpis?.todayTransfers ?? 0}</div>
                <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">Sprint 4</span>
              </div>
            )}
            <p className="mt-1 text-xs text-muted-foreground">Completed fund transfers</p>
          </CardContent>
        </Card>

        {/* Today's Volume */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Today's Volume</CardTitle>
            <DollarSign className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              <div className="flex items-baseline gap-2">
                <div className="text-2xl font-bold">
                  {kpis ? formatMoney(kpis.todayVolume, kpis.displayCurrency || "USD") : "$0.00"}
                </div>
                <span className="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground">Sprint 4</span>
              </div>
            )}
            <p className="mt-1 text-xs text-muted-foreground">Transfer volume across system</p>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
