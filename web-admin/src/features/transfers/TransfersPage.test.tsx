import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { TransfersPage } from "@/features/transfers/TransfersPage"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

const TRANSFER_PAGE = {
  content: [
    {
      id: "t0000000-0000-0000-0000-000000000001",
      reference: "TR-2026-0001",
      fromAccountId: "a0000000-0000-0000-0000-000000000001",
      fromAccountNumber: "100000000001",
      toAccountId: "a0000000-0000-0000-0000-000000000002",
      toAccountNumber: "100000000002",
      externalRef: null,
      amount: 150.0,
      currency: "USD",
      status: "COMPLETED",
      description: "Savings deposit",
      createdAt: "2026-08-19T01:30:00Z",
    },
    {
      id: "t0000000-0000-0000-0000-000000000002",
      reference: "TR-2026-0002",
      fromAccountId: "a0000000-0000-0000-0000-000000000001",
      fromAccountNumber: "100000000001",
      toAccountId: null,
      toAccountNumber: null,
      externalRef: "MERCHANT:COFFEE_SHOP",
      amount: 40000.0,
      currency: "KHR",
      status: "FAILED",
      description: "Coffee QR Payment",
      createdAt: "2026-08-19T02:00:00Z",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

function jsonResponse(body: unknown) {
  return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body) } as Response)
}

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() => jsonResponse(TRANSFER_PAGE)),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("loads transfers into the table", async () => {
  render(<TransfersPage />)

  expect(await screen.findByText("TR-2026-0001")).toBeInTheDocument()
  expect(screen.getByText("TR-2026-0002")).toBeInTheDocument()
  expect(screen.getByText("$150.00")).toBeInTheDocument()
  expect(screen.getByText("KHR 40,000")).toBeInTheDocument()
  expect(screen.getByText("completed")).toBeInTheDocument()
  expect(screen.getByText("failed")).toBeInTheDocument()
  expect(screen.getByText("Own Account")).toBeInTheDocument()
  expect(screen.getByText("QR Merchant")).toBeInTheDocument()
  expect(screen.getByText("2 transfers recorded")).toBeInTheDocument()
})

test("filters by status", async () => {
  const user = userEvent.setup()
  render(<TransfersPage />)
  await screen.findByText("TR-2026-0001")

  await user.selectOptions(screen.getByLabelText(/filter by status/i), "FAILED")

  await waitFor(() => {
    const urls = vi.mocked(fetch).mock.calls.map((call) => String(call[0]))
    expect(urls.some((url) => url.includes("status=FAILED"))).toBe(true)
  })
})

test("filters by account number", async () => {
  const user = userEvent.setup()
  render(<TransfersPage />)
  await screen.findByText("TR-2026-0001")

  await user.type(screen.getByLabelText(/filter by account/i), "100000000001")

  await waitFor(() => {
    const urls = vi.mocked(fetch).mock.calls.map((call) => String(call[0]))
    expect(urls.some((url) => url.includes("account=100000000001"))).toBe(true)
  })
})

test("filters by amount range", async () => {
  const user = userEvent.setup()
  render(<TransfersPage />)
  await screen.findByText("TR-2026-0001")

  await user.type(screen.getByLabelText(/filter by min amount/i), "100")
  await user.type(screen.getByLabelText(/filter by max amount/i), "500")

  await waitFor(() => {
    const urls = vi.mocked(fetch).mock.calls.map((call) => String(call[0]))
    expect(urls.some((url) => url.includes("minAmount=100") && url.includes("maxAmount=500"))).toBe(true)
  })
})

test("resets filters on Reset Filters click", async () => {
  const user = userEvent.setup()
  render(<TransfersPage />)
  await screen.findByText("TR-2026-0001")

  await user.selectOptions(screen.getByLabelText(/filter by status/i), "FAILED")
  const resetButton = await screen.findByRole("button", { name: /reset filters/i })
  expect(resetButton).toBeInTheDocument()

  await user.click(resetButton)

  await waitFor(() => {
    expect(screen.getByLabelText(/filter by status/i)).toHaveValue("ALL")
    expect(screen.queryByRole("button", { name: /reset filters/i })).not.toBeInTheDocument()
  })
})

test("handles fetch error with retry button", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve(null) } as Response)),
  )

  render(<TransfersPage />)
  expect(await screen.findByText(/couldn't load transfers feed/i)).toBeInTheDocument()
  expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument()
})
