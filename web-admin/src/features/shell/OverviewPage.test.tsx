import { render, screen } from "@testing-library/react"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { OverviewPage } from "@/features/shell/OverviewPage"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

const KPI_MOCK = {
  totalCustomers: 120,
  totalAccounts: 250,
  failedLogins: 5,
  todayTransfers: 42,
  todayVolume: 12500.5,
  displayCurrency: "USD",
}

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve(KPI_MOCK),
      } as Response),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("renders KPI metric cards accurately including unstubbed transfer KPIs", async () => {
  render(<OverviewPage />)

  expect(await screen.findByText("120")).toBeInTheDocument()
  expect(screen.getByText("250")).toBeInTheDocument()
  expect(screen.getByText("5")).toBeInTheDocument()
  expect(screen.getByText("42")).toBeInTheDocument()
  expect(screen.getByText("$12,500.50")).toBeInTheDocument()
  expect(screen.getByText("Total Customers")).toBeInTheDocument()
  expect(screen.getByText("Total Accounts")).toBeInTheDocument()
  expect(screen.getByText("Failed Logins")).toBeInTheDocument()
  expect(screen.getByText("Today's Transfers")).toBeInTheDocument()
  expect(screen.getByText("Today's Transfer Volume")).toBeInTheDocument()
  expect(screen.queryByText("Sprint 4")).not.toBeInTheDocument()
})

test("handles KPI fetch failure and retry", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        ok: false,
        status: 500,
        json: () => Promise.resolve(null),
      } as Response),
    ),
  )

  render(<OverviewPage />)
  expect(await screen.findByText(/couldn't load dashboard overview kpis/i)).toBeInTheDocument()
  expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument()
})
