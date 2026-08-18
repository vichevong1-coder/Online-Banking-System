import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { SettingsPage } from "@/features/settings/SettingsPage"
import { toast } from "sonner"

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "test-token" }),
}))

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 204,
        json: () => Promise.resolve(null),
      } as Response),
    ),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

test("validates password matching and min length", async () => {
  const user = userEvent.setup()
  render(<SettingsPage />)

  await user.type(screen.getByLabelText(/^current password/i), "oldpass123")
  await user.type(screen.getByLabelText(/^new password/i), "short")
  await user.type(screen.getByLabelText(/^confirm new password/i), "short")
  await user.click(screen.getByRole("button", { name: /update password/i }))

  expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument()
})

test("submits change password successfully", async () => {
  const user = userEvent.setup()
  render(<SettingsPage />)

  await user.type(screen.getByLabelText(/^current password/i), "OldPassword123")
  await user.type(screen.getByLabelText(/^new password/i), "NewPassword456")
  await user.type(screen.getByLabelText(/^confirm new password/i), "NewPassword456")
  await user.click(screen.getByRole("button", { name: /update password/i }))

  await waitFor(() => {
    expect(toast.success).toHaveBeenCalledWith("Password changed successfully.")
  })
})
