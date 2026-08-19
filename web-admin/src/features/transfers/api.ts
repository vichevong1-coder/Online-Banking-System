import { authFetch } from "@/lib/api-client"
import type { PageResponse } from "@/features/customers/api"

export type TransferStatus = "COMPLETED" | "FAILED" | "PENDING"

export type TransferItem = {
  id: string
  reference: string
  fromAccountId: string
  fromAccountNumber: string
  toAccountId: string | null
  toAccountNumber: string | null
  externalRef: string | null
  amount: number
  currency: "USD" | "KHR" | string
  status: TransferStatus
  description: string | null
  createdAt: string
  type?: string
}

export type TransferFilterParams = {
  page?: number
  size?: number
  startDate?: string
  endDate?: string
  status?: string
  minAmount?: string | number
  maxAmount?: string | number
  account?: string
  currency?: string
}

export function getTransfers(
  accessToken: string,
  params: TransferFilterParams = {},
): Promise<PageResponse<TransferItem>> {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set("page", String(params.page))
  if (params.size !== undefined) query.set("size", String(params.size))
  if (params.startDate) query.set("startDate", params.startDate)
  if (params.endDate) query.set("endDate", params.endDate)
  if (params.status && params.status !== "ALL") query.set("status", params.status)
  if (params.minAmount !== undefined && params.minAmount !== "") query.set("minAmount", String(params.minAmount))
  if (params.maxAmount !== undefined && params.maxAmount !== "") query.set("maxAmount", String(params.maxAmount))
  if (params.account) query.set("account", params.account)
  if (params.currency && params.currency !== "ALL") query.set("currency", params.currency)

  const queryString = query.toString()
  return authFetch<PageResponse<TransferItem>>(`/admin/transfers${queryString ? `?${queryString}` : ""}`, accessToken)
}

const ZERO_DECIMAL_CURRENCIES = new Set(["KHR"])

export function formatMoney(amount: number, currency: string): string {
  const fractionDigits = ZERO_DECIMAL_CURRENCIES.has(currency) ? 0 : 2
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currency || "USD",
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(amount)
}

export function formatDateTime(iso: string): string {
  try {
    const d = new Date(iso)
    if (isNaN(d.getTime())) return iso
    return d.toLocaleString("en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    })
  } catch {
    return iso
  }
}

export function getTransferType(transfer: TransferItem): string {
  if (transfer.type) {
    if (transfer.type === "QR_MERCHANT") return "QR Merchant"
    if (transfer.type === "QR_P2P") return "QR P2P"
    if (transfer.type === "EXTERNAL") return "External"
    if (transfer.type === "OWN_ACCOUNT" || transfer.type === "INTERNAL") return "Own Account"
    return transfer.type
  }

  if (transfer.externalRef) {
    const ref = transfer.externalRef.toLowerCase()
    const desc = (transfer.description || "").toLowerCase()
    if (ref.includes("merchant") || desc.includes("merchant")) return "QR Merchant"
    if (ref.includes("p2p") || ref.startsWith("khqr") || desc.includes("qr")) return "QR P2P"
    return "External"
  }

  return "Own Account"
}
