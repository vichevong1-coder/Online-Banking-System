import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { BillProvidersPage } from "@/features/bill-providers/BillProvidersPage"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const PROVIDERS_MOCK = [
  {
    id: "f0000000-0000-0000-0000-000000000001",
    name: "EDC",
    category: "ELECTRICITY",
    accountNumberPattern: "^[0-9]{8,12}$",
    active: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
  {
    id: "f0000000-0000-0000-0000-000000000002",
    name: "PPWSA",
    category: "WATER",
    accountNumberPattern: "^[0-9]{8,10}$",
    active: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
]

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve(PROVIDERS_MOCK),
      } as Response),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("renders bill providers in table", async () => {
  render(<BillProvidersPage />)

  expect(await screen.findByText("EDC")).toBeInTheDocument()
  expect(screen.getByText("PPWSA")).toBeInTheDocument()
  expect(screen.getAllByText("ELECTRICITY").length).toBeGreaterThan(0)
  expect(screen.getAllByText("WATER").length).toBeGreaterThan(0)
})

test("opens Add Provider dialog", async () => {
  const user = userEvent.setup()
  render(<BillProvidersPage />)

  await user.click(await screen.findByRole("button", { name: /add provider/i }))
  expect(screen.getByLabelText(/provider name/i)).toBeInTheDocument()
  expect(screen.getByLabelText(/account number pattern/i)).toBeInTheDocument()
})
