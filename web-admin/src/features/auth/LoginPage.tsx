import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Eye, EyeOff, Landmark, Loader2 } from "lucide-react"
import { toast } from "sonner"
import { useNavigate } from "react-router"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { describeAuthError, login } from "@/features/auth/api"
import { useAuth } from "@/features/auth/auth-context"

const loginSchema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
})

type LoginFormValues = z.infer<typeof loginSchema>

// US-010: staff/admin login. Staff accounts are `users` rows with
// role=ADMIN, identified by email — customers (US-007) never have an email
// and log in with phone instead; admin keeps a phone too, but only for
// SMS-based 2FA delivery (US-012), not for sign-in. On success the backend
// issues a short-lived challenge token and the admin is routed to the 2FA
// screen (mandatory for admin accounts) before a full session is granted —
// this component only owns the email/password step.
export function LoginPage() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const { beginTwoFactor } = useAuth()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  })

  async function onSubmit(values: LoginFormValues) {
    setIsSubmitting(true)
    try {
      const { challengeToken } = await login({ email: values.email }, values.password)
      beginTwoFactor(values.email, challengeToken)
      navigate("/verify-2fa")
    } catch (error) {
      toast.error(describeAuthError(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-8 bg-zinc-50 p-4 dark:bg-zinc-950">
      <div className="flex flex-col items-center gap-3 text-center">
        <div className="flex size-12 items-center justify-center rounded-xl border border-border bg-card shadow-sm">
          <Landmark className="size-6" />
        </div>
        <div className="flex flex-col gap-1">
          <h1 className="font-heading text-2xl font-semibold text-foreground">
            Online Banking Admin
          </h1>
          <p className="text-sm text-muted-foreground">Sign in to continue</p>
        </div>
      </div>

      <Card className="w-full max-w-sm rounded-xl border border-border shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <CardContent className="flex flex-col gap-5 pt-6">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="username"
                placeholder="you@bank.com"
                aria-invalid={!!errors.email}
                {...register("email")}
              />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email.message}</p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Password</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="pr-9"
                  aria-invalid={!!errors.password}
                  {...register("password")}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  className="absolute inset-y-0 right-0 flex items-center px-2.5 text-muted-foreground hover:text-foreground"
                  tabIndex={-1}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>

            <Separator />
          </CardContent>

          <CardFooter className="flex flex-col border-t-0 bg-transparent pt-0">
            <Button
              type="submit"
              className="h-11 w-full rounded-lg font-medium"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  Signing in...
                </>
              ) : (
                "Sign in"
              )}
            </Button>
          </CardFooter>
        </form>
      </Card>

      <p className="text-center text-xs text-muted-foreground">
        Protected by two-factor authentication
      </p>
    </div>
  )
}
