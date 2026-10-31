import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { toast } from "sonner"

import { KycPage } from "./KycPage"
import * as kycApi from "./api"
import type { KycApplication, PendingKycSummary } from "./api"
import type { PageResponse } from "@/features/customers/api"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock("./api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./api")>()
  return {
    ...actual,
    getPendingKyc: vi.fn(),
    getKycApplication: vi.fn(),
    getKycDocumentImage: vi.fn(),
    approveKyc: vi.fn(),
    rejectKyc: vi.fn(),
  }
})

const MOCK_PENDING_APPLICATIONS: PendingKycSummary[] = [
  {
    userId: "user-1",
    firstName: "Sokha",
    lastName: "Chan",
    phone: "+85512345678",
    registeredAt: "2026-10-15T08:30:00Z",
    documentCount: 3,
  },
  {
    userId: "user-2",
    firstName: "Dara",
    lastName: "Som",
    phone: "+85587654321",
    registeredAt: "2026-10-20T14:15:00Z",
    documentCount: 2,
  },
]

const MOCK_PENDING_RESPONSE: PageResponse<PendingKycSummary> = {
  content: MOCK_PENDING_APPLICATIONS,
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

const MOCK_APPLICATION_COMPLETE: KycApplication = {
  userId: "user-1",
  firstName: "Sokha",
  lastName: "Chan",
  phone: "+85512345678",
  nidNumber: "012345678",
  nidExpiryDate: "2030-01-15T00:00:00Z",
  dateOfBirth: "1995-05-25T00:00:00Z",
  gender: "MALE",
  status: "PENDING_APPROVAL",
  rejectionReason: null,
  submittedAt: "2026-10-15T08:30:00Z",
  documents: [
    {
      type: "NID_FRONT",
      sizeBytes: 102400,
      sha256: "hash-front",
      uploadedAt: "2026-10-15T08:30:00Z",
    },
    {
      type: "NID_BACK",
      sizeBytes: 102400,
      sha256: "hash-back",
      uploadedAt: "2026-10-15T08:31:00Z",
    },
    {
      type: "SELFIE",
      sizeBytes: 204800,
      sha256: "hash-selfie",
      uploadedAt: "2026-10-15T08:32:00Z",
    },
  ],
}

const MOCK_APPLICATION_INCOMPLETE: KycApplication = {
  userId: "user-2",
  firstName: "Dara",
  lastName: "Som",
  phone: "+85587654321",
  nidNumber: "987654321",
  nidExpiryDate: "2028-12-31T00:00:00Z",
  dateOfBirth: "1998-11-10T00:00:00Z",
  gender: "FEMALE",
  status: "PENDING_APPROVAL",
  rejectionReason: null,
  submittedAt: "2026-10-20T14:15:00Z",
  documents: [
    {
      type: "NID_FRONT",
      sizeBytes: 102400,
      sha256: "hash-front-2",
      uploadedAt: "2026-10-20T14:15:00Z",
    },
    {
      type: "NID_BACK",
      sizeBytes: 102400,
      sha256: "hash-back-2",
      uploadedAt: "2026-10-20T14:16:00Z",
    },
  ],
}

describe("KycPage", () => {
  let createdObjectUrls: string[] = []

  beforeEach(() => {
    createdObjectUrls = []
    let counter = 0
    globalThis.URL.createObjectURL = vi.fn(() => {
      counter++
      const url = `blob:http://localhost/test-blob-${counter}`
      createdObjectUrls.push(url)
      return url
    })
    globalThis.URL.revokeObjectURL = vi.fn()

    vi.mocked(kycApi.getPendingKyc).mockResolvedValue(MOCK_PENDING_RESPONSE)
    vi.mocked(kycApi.getKycApplication).mockImplementation((_token, userId) => {
      if (userId === "user-2") {
        return Promise.resolve(MOCK_APPLICATION_INCOMPLETE)
      }
      return Promise.resolve(MOCK_APPLICATION_COMPLETE)
    })
    vi.mocked(kycApi.getKycDocumentImage).mockResolvedValue(
      new Blob(["fake-image-data"], { type: "image/jpeg" }),
    )
    vi.mocked(kycApi.approveKyc).mockResolvedValue({
      ...MOCK_APPLICATION_COMPLETE,
      status: "ACTIVE",
    })
    vi.mocked(kycApi.rejectKyc).mockResolvedValue({
      ...MOCK_APPLICATION_COMPLETE,
      status: "REJECTED",
      rejectionReason: "Invalid document",
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it("renders pending KYC applications table with columns, document count display, and formatted dates", async () => {
    render(<KycPage />)

    expect(screen.getByRole("heading", { name: /kyc review/i })).toBeInTheDocument()
    expect(await screen.findByText("2 applications waiting for review")).toBeInTheDocument()

    // Table column headers
    expect(screen.getByRole("columnheader", { name: "Name" })).toBeInTheDocument()
    expect(screen.getByRole("columnheader", { name: "Phone" })).toBeInTheDocument()
    expect(screen.getByRole("columnheader", { name: "Documents" })).toBeInTheDocument()
    expect(screen.getByRole("columnheader", { name: "Registered" })).toBeInTheDocument()

    // Row 1: Sokha Chan (3 of 3 docs)
    expect(screen.getByText("Sokha Chan")).toBeInTheDocument()
    expect(screen.getByText("+85512345678")).toBeInTheDocument()
    const docCountComplete = screen.getByText("3 of 3")
    expect(docCountComplete).toBeInTheDocument()
    expect(docCountComplete.className).toContain("text-muted-foreground")
    expect(screen.getByText("15 Oct 2026")).toBeInTheDocument()

    // Row 2: Dara Som (2 of 3 docs)
    expect(screen.getByText("Dara Som")).toBeInTheDocument()
    expect(screen.getByText("+85587654321")).toBeInTheDocument()
    const docCountIncomplete = screen.getByText("2 of 3")
    expect(docCountIncomplete).toBeInTheDocument()
    expect(docCountIncomplete.className).toContain("text-destructive")
    expect(screen.getByText("20 Oct 2026")).toBeInTheDocument()
  })

  it("clicking a row opens review dialog and loads application details and document images via blob URL", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))

    expect(kycApi.getKycApplication).toHaveBeenCalledWith("test-token", "user-1")

    const dialog = await screen.findByRole("dialog")
    expect(within(dialog).getByText("Sokha Chan")).toBeInTheDocument()
    expect(within(dialog).getByText(/\+85512345678 · registered 15 Oct 2026/)).toBeInTheDocument()

    // Application details dl
    expect(within(dialog).getByText("National ID number")).toBeInTheDocument()
    expect(within(dialog).getByText("012345678")).toBeInTheDocument()
    expect(within(dialog).getByText("ID expiry")).toBeInTheDocument()
    expect(within(dialog).getByText("15 Jan 2030")).toBeInTheDocument()
    expect(within(dialog).getByText("Date of birth")).toBeInTheDocument()
    expect(within(dialog).getByText("25 May 1995")).toBeInTheDocument()
    expect(within(dialog).getByText("Gender")).toBeInTheDocument()
    expect(within(dialog).getByText("MALE")).toBeInTheDocument()

    // Document previews
    await waitFor(() => {
      expect(kycApi.getKycDocumentImage).toHaveBeenCalledWith("test-token", "user-1", "NID_FRONT")
      expect(kycApi.getKycDocumentImage).toHaveBeenCalledWith("test-token", "user-1", "NID_BACK")
      expect(kycApi.getKycDocumentImage).toHaveBeenCalledWith("test-token", "user-1", "SELFIE")
    })

    const frontImg = await within(dialog).findByRole("img", { name: "National ID (front)" })
    const backImg = within(dialog).getByRole("img", { name: "National ID (back)" })
    const selfieImg = within(dialog).getByRole("img", { name: "Selfie" })

    expect(frontImg).toHaveAttribute("src", expect.stringContaining("blob:http://localhost/test-blob-"))
    expect(backImg).toHaveAttribute("src", expect.stringContaining("blob:http://localhost/test-blob-"))
    expect(selfieImg).toHaveAttribute("src", expect.stringContaining("blob:http://localhost/test-blob-"))

    expect(within(dialog).getByRole("button", { name: /approve/i })).toBeEnabled()
    expect(within(dialog).getByRole("button", { name: /reject/i })).toBeEnabled()
  })

  it("approving an application calls approveKyc and triggers success toast and refreshes table", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    const approveButton = await within(dialog).findByRole("button", { name: /approve/i })
    await user.click(approveButton)

    await waitFor(() => {
      expect(kycApi.approveKyc).toHaveBeenCalledWith("test-token", "user-1")
      expect(toast.success).toHaveBeenCalledWith("Sokha Chan was approved.")
    })

    // Dialog closes
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    })

    // Table refreshed
    expect(kycApi.getPendingKyc).toHaveBeenCalledTimes(2)
  })

  it("rejecting an application shows reason textarea, validates required reason, calls rejectKyc, triggers success toast, and refreshes table", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    const rejectButton = await within(dialog).findByRole("button", { name: /^reject$/i })
    await user.click(rejectButton)

    // Reason textarea and action buttons are visible
    const reasonTextarea = within(dialog).getByLabelText(/reason shown to the customer/i)
    expect(reasonTextarea).toBeInTheDocument()
    expect(within(dialog).getByText("0/500")).toBeInTheDocument()

    const confirmRejectBtn = within(dialog).getByRole("button", { name: /confirm rejection/i })
    expect(confirmRejectBtn).toBeDisabled()

    // Whitespace only should remain disabled
    await user.type(reasonTextarea, "     ")
    expect(confirmRejectBtn).toBeDisabled()

    // Valid rejection reason
    await user.clear(reasonTextarea)
    await user.type(reasonTextarea, "NID photo is too blurry")
    expect(within(dialog).getByText("23/500")).toBeInTheDocument()
    expect(confirmRejectBtn).toBeEnabled()

    await user.click(confirmRejectBtn)

    await waitFor(() => {
      expect(kycApi.rejectKyc).toHaveBeenCalledWith("test-token", "user-1", "NID photo is too blurry")
      expect(toast.success).toHaveBeenCalledWith("Sokha Chan was rejected.")
    })

    // Dialog closes
    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    })

    // Table refreshed
    expect(kycApi.getPendingKyc).toHaveBeenCalledTimes(2)
  })

  it("allows backing out of rejection mode", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    await user.click(await within(dialog).findByRole("button", { name: /^reject$/i }))
    expect(within(dialog).getByLabelText(/reason shown to the customer/i)).toBeInTheDocument()

    // Click Back
    await user.click(within(dialog).getByRole("button", { name: /back/i }))

    expect(within(dialog).queryByLabelText(/reason shown to the customer/i)).not.toBeInTheDocument()
    expect(within(dialog).getByRole("button", { name: /^reject$/i })).toBeInTheDocument()
    expect(within(dialog).getByRole("button", { name: /approve/i })).toBeInTheDocument()
  })

  it("handles incomplete documents: Approve button is disabled if documents < 3", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Dara Som"))
    const dialog = await screen.findByRole("dialog")

    expect(
      await within(dialog).findByText(
        "This application has 2 of 3 documents. Reject it so the customer can register again.",
      ),
    ).toBeInTheDocument()

    // Missing selfie displays "Not uploaded"
    expect(within(dialog).getByText("Not uploaded")).toBeInTheDocument()

    const approveButton = within(dialog).getByRole("button", { name: /approve/i })
    expect(approveButton).toBeDisabled()

    // Reject button remains enabled
    const rejectButton = within(dialog).getByRole("button", { name: /^reject$/i })
    expect(rejectButton).toBeEnabled()
  })

  it("handles fetch error on pending list", async () => {
    vi.mocked(kycApi.getPendingKyc).mockRejectedValueOnce(new Error("Network error"))

    render(<KycPage />)

    expect(
      await screen.findByText("Couldn't load pending applications. Please try again."),
    ).toBeInTheDocument()
  })

  it("handles dialog load error when getKycApplication fails", async () => {
    vi.mocked(kycApi.getKycApplication).mockRejectedValueOnce(new Error("Failed to load"))

    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    expect(await within(dialog).findByText("Couldn't load this application.")).toBeInTheDocument()
  })

  it("handles document image load error", async () => {
    vi.mocked(kycApi.getKycDocumentImage).mockRejectedValue(new Error("Failed to load image"))

    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    await waitFor(() => {
      const errorTexts = within(dialog).getAllByText("Couldn't load image")
      expect(errorTexts.length).toBe(3)
    })
  })

  it("handles failure when approving an application", async () => {
    vi.mocked(kycApi.approveKyc).mockRejectedValueOnce(new Error("Approval failed"))

    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    const approveButton = await within(dialog).findByRole("button", { name: /approve/i })
    await user.click(approveButton)

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Couldn't approve this application.")
    })
  })

  it("handles failure when rejecting an application", async () => {
    vi.mocked(kycApi.rejectKyc).mockRejectedValueOnce(new Error("Rejection failed"))

    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    await user.click(await within(dialog).findByRole("button", { name: /^reject$/i }))
    await user.type(within(dialog).getByLabelText(/reason shown to the customer/i), "Blurry ID")
    await user.click(within(dialog).getByRole("button", { name: /confirm rejection/i }))

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Couldn't reject this application.")
    })
  })

  it("shows empty state when no applications are pending", async () => {
    vi.mocked(kycApi.getPendingKyc).mockResolvedValueOnce({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    render(<KycPage />)

    expect(await screen.findByText("0 applications waiting for review")).toBeInTheDocument()
    expect(screen.getByText("No applications are waiting for review.")).toBeInTheDocument()
  })

  it("revokes object URLs when dialog unmounts", async () => {
    const user = userEvent.setup()
    render(<KycPage />)

    await user.click(await screen.findByText("Sokha Chan"))
    const dialog = await screen.findByRole("dialog")

    await within(dialog).findByRole("img", { name: "National ID (front)" })
    expect(createdObjectUrls.length).toBe(3)

    // Close the dialog using Radix close button
    const closeBtn = within(dialog).getByRole("button", { name: /close/i })
    await user.click(closeBtn)

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    })

    expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledTimes(3)
  })
})
