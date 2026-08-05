import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Loader2, ShieldCheck } from "lucide-react"
import { toast } from "sonner"
import { Navigate, useNavigate } from "react-router"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  ApiError,
  decodeJwtExpiryMs,
  describeAuthError,
  resendTwoFactor,
  verifyTwoFactor,
} from "@/features/auth/api"
import { useAuth } from "@/features/auth/auth-context"

const codeSchema = z.object({
  code: z
    .string()
    .length(6, "Enter the 6-digit code")
    .regex(/^\d{6}$/, "Code must be 6 digits"),
})

type CodeFormValues = z.infer<typeof codeSchema>

// A resend has no backend rate limit (OTP TTL already bounds how often a new one is useful) —
// this is purely a client-side guard against double-clicks / impatient re-sends.
const RESEND_COOLDOWN_SECONDS = 30

function formatCountdown(ms: number) {
  const totalSeconds = Math.ceil(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, "0")}`
}

// US-012: 2FA enforcement for admin accounts. Delivery is SMS to the phone
// number on file for the account (no email channel — see sprint-plan.md
// "OTP & SMS delivery") — the admin signs in with email (US-010), so the
// destination phone isn't something this screen collects or knows ahead of
// the backend response. This screen only owns the code-entry step; there is
// no path into the app shell that skips it.
export function TwoFactorPage() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isResending, setIsResending] = useState(false)
  const [resendCooldown, setResendCooldown] = useState(0)
  const [remainingMs, setRemainingMs] = useState<number | null>(null)
  const navigate = useNavigate()
  const { status, email, challengeToken, beginTwoFactor, completeSignIn, signOut } = useAuth()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CodeFormValues>({
    resolver: zodResolver(codeSchema),
    defaultValues: { code: "" },
  })

  // Ticks down to the challenge token's real expiry (US-011/US-012's 2FA window) rather than a
  // hardcoded duration, so it stays correct if JWT_CHALLENGE_TOKEN_TTL is ever changed.
  useEffect(() => {
    if (!challengeToken) {
      return
    }
    const expiresAt = decodeJwtExpiryMs(challengeToken)
    if (expiresAt === null) {
      return
    }
    const tick = () => setRemainingMs(Math.max(0, expiresAt - Date.now()))
    tick()
    const interval = setInterval(tick, 1000)
    return () => clearInterval(interval)
  }, [challengeToken])

  useEffect(() => {
    if (resendCooldown <= 0) {
      return
    }
    const interval = setInterval(() => setResendCooldown((seconds) => Math.max(0, seconds - 1)), 1000)
    return () => clearInterval(interval)
  }, [resendCooldown])

  if (status === "signed-out") {
    return <Navigate to="/login" replace />
  }

  if (status === "signed-in") {
    return <Navigate to="/" replace />
  }

  const isExpired = remainingMs === 0

  async function onSubmit(values: CodeFormValues) {
    if (!challengeToken) {
      return
    }
    setIsSubmitting(true)
    try {
      const tokens = await verifyTwoFactor(challengeToken, values.code)
      completeSignIn(tokens)
      navigate("/")
    } catch (error) {
      if (error instanceof ApiError && error.code === "INVALID_OR_EXPIRED_CHALLENGE") {
        toast.error(describeAuthError(error))
        signOut()
        navigate("/login")
        return
      }
      toast.error(describeAuthError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleResend() {
    if (!challengeToken || !email) {
      return
    }
    setIsResending(true)
    try {
      const response = await resendTwoFactor(challengeToken)
      beginTwoFactor(email, response.challengeToken)
      toast.success("A new code is on its way.")
      setResendCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (error) {
      if (error instanceof ApiError && error.code === "INVALID_OR_EXPIRED_CHALLENGE") {
        toast.error(describeAuthError(error))
        signOut()
        navigate("/login")
        return
      }
      toast.error(describeAuthError(error))
    } finally {
      setIsResending(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted/30 p-4">
      <div className="flex items-center gap-2 text-foreground">
        <ShieldCheck className="size-6" />
        <h1 className="font-heading text-lg font-semibold">Two-factor verification</h1>
      </div>

      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Enter verification code</CardTitle>
          <CardDescription>
            {email
              ? `We sent a 6-digit code by SMS to the phone on file for ${email}.`
              : "We sent a 6-digit code by SMS to the phone number on file."}
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <CardContent className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="code">Verification code</Label>
              <Input
                id="code"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder="123456"
                aria-invalid={!!errors.code}
                disabled={isExpired}
                {...register("code")}
              />
              {errors.code && (
                <p className="text-sm text-destructive">{errors.code.message}</p>
              )}
            </div>

            <div className="flex items-center justify-between text-sm">
              <span className={isExpired ? "text-destructive" : "text-muted-foreground"}>
                {remainingMs === null
                  ? null
                  : isExpired
                    ? "Code expired."
                    : `Expires in ${formatCountdown(remainingMs)}`}
              </span>
              <Button
                type="button"
                variant="link"
                size="default"
                className="h-auto p-0"
                disabled={isResending || resendCooldown > 0}
                onClick={handleResend}
              >
                {isResending
                  ? "Sending..."
                  : resendCooldown > 0
                    ? `Resend code (${resendCooldown}s)`
                    : "Resend code"}
              </Button>
            </div>
          </CardContent>

          <CardFooter className="flex flex-col gap-4 border-t-0 bg-transparent pt-0">
            <Button type="submit" className="w-full" disabled={isSubmitting || isExpired}>
              {isSubmitting && <Loader2 className="size-4 animate-spin" />}
              Verify
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
