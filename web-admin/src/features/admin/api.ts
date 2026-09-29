import { authFetch } from "@/lib/api-client"

export type Role = "CUSTOMER" | "ADMIN" | "TELLER"
export type AccountStatus = "ACTIVE" | "SUSPENDED" | "LOCKED"

export type KpiData = {
  totalCustomers: number
  totalAccounts: number
  failedLogins: number
  todayTransfers: number
  todayVolume: number
  displayCurrency: string
}

export type StaffMember = {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  role: Role
  status: AccountStatus
  createdAt: string
}

export type CreateStaffPayload = {
  firstName: string
  lastName: string
  email: string
  phone: string
  role: Role
  password: string
}

export type ChangePasswordPayload = {
  currentPassword: string
  newPassword: string
}

export function getKpis(accessToken: string): Promise<KpiData> {
  return authFetch<KpiData>("/admin/kpis", accessToken)
}

export function getStaff(accessToken: string): Promise<StaffMember[]> {
  return authFetch<StaffMember[]>("/admin/staff", accessToken)
}

export function createStaff(accessToken: string, payload: CreateStaffPayload): Promise<StaffMember> {
  return authFetch<StaffMember>("/admin/staff", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export function updateStaffRole(accessToken: string, staffId: string, role: Role): Promise<StaffMember> {
  return authFetch<StaffMember>(`/admin/staff/${staffId}/role`, accessToken, {
    method: "PATCH",
    body: JSON.stringify({ role }),
  })
}

export function changePassword(accessToken: string, payload: ChangePasswordPayload): Promise<void> {
  return authFetch<void>("/admin/me/password", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}
