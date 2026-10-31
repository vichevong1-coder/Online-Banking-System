import { BrowserRouter, Navigate, Route, Routes } from "react-router"
import { Toaster } from "sonner"

import { AuthProvider } from "@/features/auth/auth-context"
import { LoginPage } from "@/features/auth/LoginPage"
import { TwoFactorPage } from "@/features/auth/TwoFactorPage"
import { BillProvidersPage } from "@/features/bill-providers/BillProvidersPage"
import { ADMIN_ONLY, STAFF } from "@/features/auth/permissions"
import { CustomersPage } from "@/features/customers/CustomersPage"
import { KycPage } from "@/features/kyc/KycPage"
import { RolesPage } from "@/features/roles/RolesPage"
import { SettingsPage } from "@/features/settings/SettingsPage"
import { AppShellLayout } from "@/features/shell/AppShellLayout"
import { OverviewPage } from "@/features/shell/OverviewPage"
import { ProtectedRoute } from "@/features/shell/ProtectedRoute"
import { TransfersPage } from "@/features/transfers/TransfersPage"
import { AuditLogsPage } from "@/features/audit-logs/AuditLogsPage"

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/verify-2fa" element={<TwoFactorPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppShellLayout />}>
              <Route element={<ProtectedRoute allowedRoles={STAFF} />}>
                <Route path="/customers" element={<CustomersPage />} />
                <Route path="/kyc" element={<KycPage />} />
              </Route>
              <Route element={<ProtectedRoute allowedRoles={ADMIN_ONLY} />}>
                <Route path="/" element={<OverviewPage />} />
                <Route path="/transfers" element={<TransfersPage />} />
                <Route path="/bill-providers" element={<BillProvidersPage />} />
                <Route path="/roles" element={<RolesPage />} />
                <Route path="/audit-logs" element={<AuditLogsPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
      <Toaster />
    </AuthProvider>
  )
}

export default App
