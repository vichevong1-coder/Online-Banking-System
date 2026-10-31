import { authFetch, authFetchBlob } from "@/lib/api-client"
import type { PageResponse } from "@/features/customers/api"

// Backend: com.obs.backend.security.AccountStatus (includes the two KYC-only states).
export type KycStatus = "ACTIVE" | "SUSPENDED" | "LOCKED" | "PENDING_APPROVAL" | "REJECTED"

// Backend: com.obs.backend.feature.kyc.entity.KycDocumentType
export type KycDocumentType = "NID_FRONT" | "NID_BACK" | "SELFIE"

export const KYC_DOCUMENT_LABELS: Record<KycDocumentType, string> = {
  NID_FRONT: "National ID (front)",
  NID_BACK: "National ID (back)",
  SELFIE: "Selfie",
}

// Backend: com.obs.backend.feature.kyc.dto.PendingKycSummaryResponse
export type PendingKycSummary = {
  userId: string
  firstName: string
  lastName: string
  phone: string
  registeredAt: string
  documentCount: number
}

// Backend: com.obs.backend.feature.kyc.dto.KycDocumentInfo
export type KycDocumentInfo = {
  type: KycDocumentType
  sizeBytes: number
  sha256: string
  uploadedAt: string
}

// Backend: com.obs.backend.feature.kyc.dto.KycApplicationResponse
export type KycApplication = {
  userId: string
  firstName: string
  lastName: string
  phone: string
  nidNumber: string | null
  nidExpiryDate: string | null
  dateOfBirth: string | null
  gender: "MALE" | "FEMALE" | "OTHER" | null
  status: KycStatus
  rejectionReason: string | null
  submittedAt: string | null
  documents: KycDocumentInfo[]
}

export function getPendingKyc(accessToken: string, options: { page?: number; size?: number } = {}) {
  const params = new URLSearchParams()
  params.set("page", String(options.page ?? 0))
  params.set("size", String(options.size ?? 20))
  return authFetch<PageResponse<PendingKycSummary>>(`/admin/kyc/pending?${params}`, accessToken)
}

export function getKycApplication(accessToken: string, userId: string) {
  return authFetch<KycApplication>(`/admin/kyc/${userId}`, accessToken)
}

/** The image endpoint needs the Authorization header, so it is fetched as a blob, not via <img src>. */
export function getKycDocumentImage(accessToken: string, userId: string, type: KycDocumentType) {
  return authFetchBlob(`/admin/kyc/${userId}/documents/${type}/image`, accessToken)
}

export function approveKyc(accessToken: string, userId: string) {
  return authFetch<KycApplication>(`/admin/kyc/${userId}/approve`, accessToken, { method: "POST" })
}

export function rejectKyc(accessToken: string, userId: string, reason: string) {
  return authFetch<KycApplication>(`/admin/kyc/${userId}/reject`, accessToken, {
    method: "POST",
    body: JSON.stringify({ reason }),
  })
}
