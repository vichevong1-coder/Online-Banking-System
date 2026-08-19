import { ApiError, refresh } from "@/features/auth/api"

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"
const STORAGE_KEY = "obs-admin-session"

let activeRefreshPromise: Promise<string> | null = null

/**
 * Attempts to exchange the stored session refresh token for a fresh access token.
 * Deduplicates in-flight refresh requests across concurrent 401s.
 */
async function requestTokenRefresh(): Promise<string> {
  if (activeRefreshPromise) {
    return activeRefreshPromise
  }

  activeRefreshPromise = (async () => {
    try {
      const stored = typeof sessionStorage !== "undefined" ? sessionStorage.getItem(STORAGE_KEY) : null
      if (!stored) {
        throw new Error("No stored session available for refresh")
      }
      const session = JSON.parse(stored) as { refreshToken?: string }
      if (!session.refreshToken) {
        throw new Error("No refresh token found in session storage")
      }

      const response = await refresh(session.refreshToken)
      const newAccessToken = response.accessToken

      // Dispatch event so AuthContext can sync state
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("obs-token-refreshed", { detail: newAccessToken }))
      }

      return newAccessToken
    } catch (err) {
      if (typeof sessionStorage !== "undefined") {
        sessionStorage.removeItem(STORAGE_KEY)
      }
      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("obs-auth-expired"))
      }
      throw err
    } finally {
      activeRefreshPromise = null
    }
  })()

  return activeRefreshPromise
}

/**
 * Authenticated fetch helper that injects Bearer authorization and automatically
 * attempts a mid-session token refresh and request retry on 401 responses.
 */
export async function authFetch<T>(
  path: string,
  accessToken: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${accessToken}`,
    ...(options.headers as Record<string, string>),
  }
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json"
  }

  let response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  // Handle mid-session 401 by attempting token refresh and request retry
  if (response.status === 401) {
    try {
      const newAccessToken = await requestTokenRefresh()
      const retryHeaders: Record<string, string> = {
        ...headers,
        Authorization: `Bearer ${newAccessToken}`,
      }
      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: retryHeaders,
      })
    } catch {
      // If refresh fails, fall through to default error response parsing
    }
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw new ApiError(response.status, payload?.error ?? "UNKNOWN_ERROR")
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
