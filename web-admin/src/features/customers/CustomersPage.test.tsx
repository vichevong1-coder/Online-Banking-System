import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { CustomersPage } from "@/features/customers/CustomersPage"

// The page reads only accessToken off the auth context, so stubbing the hook avoids standing up
// AuthProvider (and its sessionStorage / refresh-on-mount behaviour) for a rendering test.
vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

// Shapes mirror the real backend responses — verified by curl against the seeded demo data.
const CUSTOMER_PAGE = {
  content: [
    {
      id: "d0000000-0000-0000-0000-000000000003",
      firstName: "Nita",
      lastName: "Pich",
      phone: "+85512000003",
      status: "ACTIVE",
      phoneVerified: true,
      createdAt: "2026-05-30T00:00:00Z",
    },
    {
      id: "d0000000-0000-0000-0000-000000000004",
      firstName: "Vibol",
      lastName: "Keo",
      phone: "+85512000004",
      status: "SUSPENDED",
      phoneVerified: true,
      createdAt: "2026-07-19T00:00:00Z",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

const CUSTOMER_DETAIL = {
  ...CUSTOMER_PAGE.content[0],
  nidNumber: "034567890",
  nidExpiryDate: "2031-01-15",
  dateOfBirth: "1999-12-02",
  gender: "FEMALE",
}

const CUSTOMER_ACCOUNTS = [
  {
    id: "a0000000-0000-0000-0000-000000000004",
    accountNumber: "900000000004",
    accountType: "CHECKING",
    currency: "USD",
    balance: 550.0,
    createdAt: "2026-05-30T00:00:00Z",
  },
  {
    id: "a0000000-0000-0000-0000-000000000005",
    accountNumber: "900000000005",
    accountType: "SAVINGS",
    currency: "KHR",
    balance: 5000000.0,
    createdAt: "2026-06-01T00:00:00Z",
  },
]

function jsonResponse(body: unknown) {
  return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve(body) } as Response)
}

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes("/accounts")) return jsonResponse(CUSTOMER_ACCOUNTS)
      if (/\/admin\/customers\/[0-9a-f-]+$/.test(url)) return jsonResponse(CUSTOMER_DETAIL)
      return jsonResponse(CUSTOMER_PAGE)
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("loads customers into the table", async () => {
  render(<CustomersPage />)

  expect(await screen.findByText("Nita Pich")).toBeInTheDocument()
  expect(screen.getByText("Vibol Keo")).toBeInTheDocument()
  expect(screen.getByText("+85512000003")).toBeInTheDocument()
  expect(screen.getByText("2 customers")).toBeInTheDocument()
})

test("shows the suspended status on the suspended customer", async () => {
  render(<CustomersPage />)

  await screen.findByText("Vibol Keo")
  expect(screen.getByText("suspended")).toBeInTheDocument()
})

// The drawer is the most conditional code on this screen — loading, empty and error branches per
// section. This drives the real path: click a row, resolve both requests, render both sections.
test("opening a customer shows their KYC data and accounts with formatted balances", async () => {
  const user = userEvent.setup()
  render(<CustomersPage />)

  await user.click(await screen.findByText("Nita Pich"))

  expect(await screen.findByText("900000000004")).toBeInTheDocument()
  expect(screen.getByText("900000000005")).toBeInTheDocument()
  // USD renders with two decimals, KHR with none — Intl drives both off the currency code.
  expect(screen.getByText("$550.00")).toBeInTheDocument()
  expect(screen.getByText("KHR 5,000,000")).toBeInTheDocument()
  expect(screen.getByText("034567890")).toBeInTheDocument()
})

test("search narrows the request sent to the backend", async () => {
  const user = userEvent.setup()
  render(<CustomersPage />)
  await screen.findByText("Nita Pich")

  await user.type(screen.getByLabelText(/search customers/i), "Nita")

  await waitFor(() => {
    const urls = vi.mocked(fetch).mock.calls.map((call) => String(call[0]))
    expect(urls.some((url) => url.includes("search=Nita"))).toBe(true)
  })
})

test("surfaces a readable error when the request fails", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve(null) } as Response)),
  )

  render(<CustomersPage />)

  expect(await screen.findByText(/couldn't load customers/i)).toBeInTheDocument()
})
