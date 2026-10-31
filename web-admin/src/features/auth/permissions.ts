import type { Role } from "@/features/admin/api"

/**
 * Which staff roles may open each web-admin route. Mirrors the backend @PreAuthorize rules:
 * ADMIN-only controllers (KPIs, transfers, bill providers, staff, self) and
 * hasAnyRole('ADMIN','TELLER') for customers and KYC.
 */
export const ADMIN_ONLY: Role[] = ["ADMIN"]
export const STAFF: Role[] = ["ADMIN", "TELLER"]

export const ROUTE_ROLES: Record<string, Role[]> = {
  "/": ADMIN_ONLY,
  "/customers": STAFF,
  "/kyc": STAFF,
  "/transfers": ADMIN_ONLY,
  "/bill-providers": ADMIN_ONLY,
  "/roles": ADMIN_ONLY,
  "/audit-logs": ADMIN_ONLY,
  "/settings": ADMIN_ONLY,
}

export function canAccess(role: Role | undefined, path: string): boolean {
  if (!role) return false
  return (ROUTE_ROLES[path] ?? ADMIN_ONLY).includes(role)
}

/** Landing page after sign-in, and the redirect target when a role opens a page it cannot use. */
export function homeFor(role: Role | undefined): string {
  return role === "TELLER" ? "/customers" : "/"
}

export const ROLE_CAPABILITIES: { role: Role; summary: string; can: string[] }[] = [
  {
    role: "ADMIN",
    summary: "Full web-admin access",
    can: [
      "Everything a teller can do",
      "KPIs overview and all transfers",
      "Bill providers and invoices",
      "Audit logs",
      "Staff and role management, own password",
    ],
  },
  {
    role: "TELLER",
    summary: "Front-desk staff, limited access",
    can: ["Customer management", "KYC review: approve or reject sign-ups"],
  },
  {
    role: "CUSTOMER",
    summary: "Mobile app only, no web-admin access",
    can: ["Own accounts, transfers, beneficiaries, cards", "Bill payments and statements", "KYC sign-up"],
  },
]
