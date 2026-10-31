import { describe, expect, it, vi, beforeEach } from "vitest"
import {
  approveKyc,
  getKycApplication,
  getKycDocumentImage,
  getPendingKyc,
  rejectKyc,
  KYC_DOCUMENT_LABELS,
} from "./api"
import { authFetch, authFetchBlob } from "@/lib/api-client"

vi.mock("@/lib/api-client", () => ({
  authFetch: vi.fn(),
  authFetchBlob: vi.fn(),
}))

describe("kyc api", () => {
  const token = "mock-token"

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("getPendingKyc uses default pagination when not provided", async () => {
    vi.mocked(authFetch).mockResolvedValueOnce({ content: [], totalElements: 0 })
    await getPendingKyc(token)

    expect(authFetch).toHaveBeenCalledWith("/admin/kyc/pending?page=0&size=20", token)
  })

  it("getPendingKyc passes custom page and size", async () => {
    vi.mocked(authFetch).mockResolvedValueOnce({ content: [], totalElements: 0 })
    await getPendingKyc(token, { page: 2, size: 50 })

    expect(authFetch).toHaveBeenCalledWith("/admin/kyc/pending?page=2&size=50", token)
  })

  it("getKycApplication requests single application by userId", async () => {
    const mockApp = { userId: "user-1", firstName: "Test" }
    vi.mocked(authFetch).mockResolvedValueOnce(mockApp)

    const result = await getKycApplication(token, "user-1")
    expect(authFetch).toHaveBeenCalledWith("/admin/kyc/user-1", token)
    expect(result).toEqual(mockApp)
  })

  it("getKycDocumentImage calls authFetchBlob with document type", async () => {
    const blob = new Blob(["test"])
    vi.mocked(authFetchBlob).mockResolvedValueOnce(blob)

    const result = await getKycDocumentImage(token, "user-1", "NID_FRONT")
    expect(authFetchBlob).toHaveBeenCalledWith("/admin/kyc/user-1/documents/NID_FRONT/image", token)
    expect(result).toBe(blob)
  })

  it("approveKyc sends POST to approve endpoint", async () => {
    vi.mocked(authFetch).mockResolvedValueOnce({ status: "ACTIVE" })

    await approveKyc(token, "user-1")
    expect(authFetch).toHaveBeenCalledWith("/admin/kyc/user-1/approve", token, {
      method: "POST",
    })
  })

  it("rejectKyc sends POST with JSON body containing rejection reason", async () => {
    vi.mocked(authFetch).mockResolvedValueOnce({ status: "REJECTED" })

    await rejectKyc(token, "user-1", "Blurry selfie")
    expect(authFetch).toHaveBeenCalledWith("/admin/kyc/user-1/reject", token, {
      method: "POST",
      body: JSON.stringify({ reason: "Blurry selfie" }),
    })
  })

  it("exports human-readable document labels", () => {
    expect(KYC_DOCUMENT_LABELS.NID_FRONT).toBe("National ID (front)")
    expect(KYC_DOCUMENT_LABELS.NID_BACK).toBe("National ID (back)")
    expect(KYC_DOCUMENT_LABELS.SELFIE).toBe("Selfie")
  })
})
