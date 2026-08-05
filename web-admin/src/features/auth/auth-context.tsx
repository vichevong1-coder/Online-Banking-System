import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react"

import { refresh, type AuthTokenResponse } from "@/features/auth/api"

// Session survives a page refresh via sessionStorage (cleared when the tab closes — a
// deliberately narrower bar than "remember me across browser restarts", which nobody asked
// for and would need its own security tradeoffs). Only the refresh token + user profile are
// persisted, not the access token: on load we exchange the stored refresh token for a fresh
// access token via /auth/refresh rather than trusting a possibly-stale cached one.
type AuthStatus = "restoring" | "signed-out" | "awaiting-2fa" | "signed-in"

type SignedInUser = {
  id: string
  firstName: string
  lastName: string
  role: AuthTokenResponse["role"]
}

type StoredSession = {
  refreshToken: string
  email: string
  user: SignedInUser
}

const STORAGE_KEY = "obs-admin-session"

function loadStoredSession(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as StoredSession) : null
  } catch {
    return null
  }
}

function saveStoredSession(session: StoredSession) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

function clearStoredSession() {
  sessionStorage.removeItem(STORAGE_KEY)
}

type AuthContextValue = {
  status: AuthStatus
  email: string | null
  challengeToken: string | null
  accessToken: string | null
  user: SignedInUser | null
  beginTwoFactor: (email: string, challengeToken: string) => void
  completeSignIn: (tokens: AuthTokenResponse) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("restoring")
  const [email, setEmail] = useState<string | null>(null)
  const [challengeToken, setChallengeToken] = useState<string | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [user, setUser] = useState<SignedInUser | null>(null)

  useEffect(() => {
    const stored = loadStoredSession()
    if (!stored) {
      setStatus("signed-out")
      return
    }

    let cancelled = false
    refresh(stored.refreshToken)
      .then((response) => {
        if (cancelled) return
        setAccessToken(response.accessToken)
        setEmail(stored.email)
        setUser(stored.user)
        setStatus("signed-in")
      })
      .catch(() => {
        if (cancelled) return
        clearStoredSession()
        setStatus("signed-out")
      })

    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      email,
      challengeToken,
      accessToken,
      user,
      beginTwoFactor: (submittedEmail, token) => {
        setEmail(submittedEmail)
        setChallengeToken(token)
        setStatus("awaiting-2fa")
      },
      completeSignIn: (tokens) => {
        const signedInUser: SignedInUser = {
          id: tokens.userId,
          firstName: tokens.firstName,
          lastName: tokens.lastName,
          role: tokens.role,
        }
        setAccessToken(tokens.accessToken)
        setUser(signedInUser)
        setChallengeToken(null)
        setStatus("signed-in")
        if (email) {
          saveStoredSession({ refreshToken: tokens.refreshToken, email, user: signedInUser })
        }
      },
      signOut: () => {
        clearStoredSession()
        setEmail(null)
        setChallengeToken(null)
        setAccessToken(null)
        setUser(null)
        setStatus("signed-out")
      },
    }),
    [status, email, challengeToken, accessToken, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider.")
  }

  return context
}
