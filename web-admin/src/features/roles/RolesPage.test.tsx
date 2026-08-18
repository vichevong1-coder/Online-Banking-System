import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { RolesPage } from "@/features/roles/RolesPage"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const STAFF_MOCK = [
  {
    id: "s0000000-0000-0000-0000-000000000001",
    firstName: "Vichea",
    lastName: "Vong",
    email: "admin@bank.com",
    phone: "+85512000001",
    role: "ADMIN",
    status: "ACTIVE",
    createdAt: "2026-01-01T00:00:00Z",
  },
]

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve(STAFF_MOCK),
      } as Response),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("renders staff list in table", async () => {
  render(<RolesPage />)

  expect(await screen.findByText("Vichea Vong")).toBeInTheDocument()
  expect(screen.getByText("admin@bank.com")).toBeInTheDocument()
  expect(screen.getByText("+85512000001")).toBeInTheDocument()
  expect(screen.getByText("ADMIN")).toBeInTheDocument()
})

test("opens Add Staff Member dialog", async () => {
  const user = userEvent.setup()
  render(<RolesPage />)

  await user.click(await screen.findByRole("button", { name: /add staff member/i }))
  expect(screen.getByLabelText(/first name/i)).toBeInTheDocument()
  expect(screen.getByLabelText(/temporary password/i)).toBeInTheDocument()
})
