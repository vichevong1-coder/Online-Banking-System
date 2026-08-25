import { authFetch } from "@/lib/api-client"
import type { BillProvider, CreateBillProviderPayload, UpdateBillProviderPayload } from "./types"

export async function getBillProviders(token: string): Promise<BillProvider[]> {
  return authFetch<BillProvider[]>("/admin/bill-providers", token)
}

export async function createBillProvider(
  token: string,
  payload: CreateBillProviderPayload,
): Promise<BillProvider> {
  return authFetch<BillProvider>("/admin/bill-providers", token, {
    method: "POST",
    body: JSON.stringify(payload),
  })
}

export async function updateBillProvider(
  token: string,
  id: string,
  payload: UpdateBillProviderPayload,
): Promise<BillProvider> {
  return authFetch<BillProvider>(`/admin/bill-providers/${id}`, token, {
    method: "PATCH",
    body: JSON.stringify(payload),
  })
}

export async function deleteBillProvider(token: string, id: string): Promise<void> {
  await authFetch<void>(`/admin/bill-providers/${id}`, token, {
    method: "DELETE",
  })
}
