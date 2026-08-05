import { Navigate, Outlet } from "react-router"
import { Loader2 } from "lucide-react"

import { useAuth } from "@/features/auth/auth-context"

export function ProtectedRoute() {
  const { status } = useAuth()

  if (status === "restoring") {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (status === "signed-out") {
    return <Navigate to="/login" replace />
  }

  if (status === "awaiting-2fa") {
    return <Navigate to="/verify-2fa" replace />
  }

  return <Outlet />
}
