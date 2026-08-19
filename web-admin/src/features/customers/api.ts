import { authFetch } from "@/lib/api-client"

// Backend: com.obs.backend.common.dto.PageResponse
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AccountStatus = "ACTIVE" | "SUSPENDED" | "LOCKED"

// Backend: com.obs.backend.feature.admin.dto.CustomerSummaryResponse (US-054).
// Note there is no email — customers are identified by phone and users.email is NULL for them
// (US-007 collects no email address). Any UI that assumes an email here will render blank.
export type CustomerSummary = {
  id: string
  firstName: string
  lastName: string
  phone: string
  status: AccountStatus
  phoneVerified: boolean
  createdAt: string
}

// Backend: com.obs.backend.feature.admin.dto.CustomerDetailResponse (US-054).
export type CustomerDetail = CustomerSummary & {
  nidNumber: string | null
  nidExpiryDate: string | null
  dateOfBirth: string | null
  gender: "MALE" | "FEMALE" | "OTHER" | null
}

// Backend: com.obs.backend.feature.account.dto.AccountResponse (US-049).
export type Account = {
  id: string
  accountNumber: string
  accountType: "SAVINGS" | "CHECKING"
  currency: "USD" | "KHR"
  balance: number
  createdAt: string
}

export function searchCustomers(
  accessToken: string,
  options: { search?: string; page?: number; size?: number } = {},
) {
  const params = new URLSearchParams()
  if (options.search) params.set("search", options.search)
  params.set("page", String(options.page ?? 0))
  params.set("size", String(options.size ?? 20))
  return authFetch<PageResponse<CustomerSummary>>(`/admin/customers?${params}`, accessToken)
}

export function getCustomer(accessToken: string, customerId: string) {
  return authFetch<CustomerDetail>(`/admin/customers/${customerId}`, accessToken)
}

export function getCustomerAccounts(accessToken: string, customerId: string) {
  return authFetch<Account[]>(`/admin/customers/${customerId}/accounts`, accessToken)
}

// US-048. Returns the updated customer so the caller can refresh the row and the drawer from the
// server's view rather than assuming the write landed as sent.
export async function updateCustomerStatus(
  accessToken: string,
  customerId: string,
  status: AccountStatus,
): Promise<CustomerDetail> {
  return authFetch<CustomerDetail>(`/admin/customers/${customerId}/status`, accessToken, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  })
}

// Riel is conventionally written without decimal places, but Intl defaults KHR to two (it returns
// "KHR 5,000,000.00"), so the fraction digits are overridden rather than left to the currency code.
// USD keeps its two. Both accounts of a multi-currency customer sit in one list, so they have to
// render side by side correctly.
const ZERO_DECIMAL_CURRENCIES = new Set(["KHR"])

export function formatMoney(amount: number, currency: string): string {
  const fractionDigits = ZERO_DECIMAL_CURRENCIES.has(currency) ? 0 : 2
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(amount)
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })
}
