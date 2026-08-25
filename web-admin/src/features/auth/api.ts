const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"

export class ApiError extends Error {
  status: number
  code: string

  constructor(status: number, code: string) {
    super(code)
    this.status = status
    this.code = code
  }
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new ApiError(response.status, payload?.error ?? "UNKNOWN_ERROR")
  }

  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

// Backend: com.obs.backend.feature.auth.dto.LoginResponse (US-009/US-010). 2FA is mandatory
// for every account — login never returns real tokens directly, only a short-lived challenge.
export type LoginResponse = { challengeToken: string }

export function login(identifier: { email: string } | { phone: string }, password: string) {
  return post<LoginResponse>("/auth/login", { ...identifier, password })
}

// Backend: com.obs.backend.feature.auth.dto.AuthTokenResponse (US-011/US-012).
export type AuthTokenResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  userId: string
  firstName: string
  lastName: string
  role: "CUSTOMER" | "ADMIN"
}

export function verifyTwoFactor(challengeToken: string, code: string) {
  return post<AuthTokenResponse>("/auth/2fa/verify", { challengeToken, code })
}

export function resendTwoFactor(challengeToken: string) {
  return post<LoginResponse>("/auth/2fa/resend", { challengeToken })
}

// Backend: com.obs.backend.feature.auth.dto.RefreshResponse (US-004).
export type RefreshResponse = { accessToken: string; tokenType: string }

export function refresh(refreshToken: string) {
  return post<RefreshResponse>("/auth/refresh", { refreshToken })
}

export function forgotPassword(identifier: string) {
  return post<void>("/auth/password/forgot", { identifier })
}

export function resetPassword(identifier: string, code: string, newPassword: string) {
  return post<void>("/auth/password/reset", { identifier, code, newPassword })
}

// The challenge token is a JWT — decoding it client-side (no signature check, this is only
// used to drive the countdown UI) reads its real expiry instead of hardcoding the TTL, so the
// timer stays correct if JWT_CHALLENGE_TOKEN_TTL is ever changed.
export function decodeJwtExpiryMs(token: string): number | null {
  try {
    const payload = token.split(".")[1]
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/")
    const claims: unknown = JSON.parse(atob(base64))
    const exp = (claims as { exp?: unknown }).exp
    return typeof exp === "number" ? exp * 1000 : null
  } catch {
    return null
  }
}

const ERROR_MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: "Incorrect email or password.",
  ACCOUNT_SUSPENDED: "This account is suspended. Contact an administrator.",
  ACCOUNT_LOCKED: "This account is locked. Contact an administrator.",
  INVALID_OR_EXPIRED_OTP: "That code is incorrect or has expired.",
  INVALID_OR_EXPIRED_CHALLENGE: "Your session expired. Please sign in again.",
  INVALID_OR_EXPIRED_REFRESH_TOKEN: "Your session expired. Please sign in again.",
}

export function describeAuthError(error: unknown): string {
  if (error instanceof ApiError) {
    return ERROR_MESSAGES[error.code] ?? "Something went wrong. Please try again."
  }
  return "Couldn't reach the server. Please try again."
}
