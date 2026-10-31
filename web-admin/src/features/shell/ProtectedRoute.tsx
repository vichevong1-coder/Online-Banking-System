import { Navigate, Outlet } from "react-router"
import { Loader2 } from "lucide-react"

import type { Role } from "@/features/admin/api"
import { useAuth } from "@/features/auth/auth-context"
import { homeFor } from "@/features/auth/permissions"

type ProtectedRouteProps = {
  /** When set, signed-in users without one of these roles are sent to their own home page. */
  allowedRoles?: Role[]
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps = {}) {
  const { status, user } = useAuth()

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

  if (allowedRoles && (!user || !allowedRoles.includes(user.role))) {
    return <Navigate to={homeFor(user?.role)} replace />
  }

  return <Outlet />
}
