export type BillCategory = "ELECTRICITY" | "WATER" | "INTERNET" | "MOBILE_TOPUP" | "OTHER"

export interface BillProvider {
  id: string
  name: string
  category: BillCategory
  accountNumberPattern: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateBillProviderPayload {
  name: string
  category: BillCategory
  accountNumberPattern?: string
  active?: boolean
}

export interface UpdateBillProviderPayload {
  name?: string
  category?: BillCategory
  accountNumberPattern?: string
  active?: boolean
}
