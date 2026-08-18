import { ApiError } from "@/features/auth/api"

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

export type Role = "CUSTOMER" | "ADMIN"
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

async function authFetch<T>(
  path: string,
  accessToken: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
    ...(options.headers as Record<string, string>),
  }
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json"
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new ApiError(response.status, payload?.error ?? "UNKNOWN_ERROR")
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
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
