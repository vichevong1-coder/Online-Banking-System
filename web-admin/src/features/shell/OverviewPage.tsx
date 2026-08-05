import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

// The KPI home screen (US-053) is Sprint 5 work against APIs that don't
// exist yet. This is a placeholder so the shell has somewhere to route on
// sign-in.
export function OverviewPage() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>You're signed in</CardTitle>
      </CardHeader>
      <CardContent className="text-sm text-muted-foreground">
        Dashboard KPIs and the rest of the admin portal land in Sprint 5.
      </CardContent>
    </Card>
  )
}
