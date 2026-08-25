import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { Eye, EyeOff, KeyRound, Landmark, Loader2 } from "lucide-react"
import { toast } from "sonner"
import { useNavigate } from "react-router"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { describeAuthError, forgotPassword, login, resetPassword } from "@/features/auth/api"
import { useAuth } from "@/features/auth/auth-context"

const loginSchema = z.object({
  email: z.string().min(1, "Email is required").email("Enter a valid email address"),
  password: z.string().min(1, "Password is required"),
})

type LoginFormValues = z.infer<typeof loginSchema>

const forgotSchema = z.object({
  identifier: z.string().min(1, "Email or phone number is required"),
})

const resetSchema = z
  .object({
    code: z.string().length(6, "Verification code must be 6 digits"),
    newPassword: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string().min(8, "Password must be at least 8 characters"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  })

export function LoginPage() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [forgotOpen, setForgotOpen] = useState(false)
  const [resetStep, setResetStep] = useState<"identifier" | "code">("identifier")
  const [savedIdentifier, setSavedIdentifier] = useState("")
  const [resetSubmitting, setResetSubmitting] = useState(false)

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

  const forgotForm = useForm<{ identifier: string }>({
    resolver: zodResolver(forgotSchema),
    defaultValues: { identifier: "" },
  })

  const resetForm = useForm<z.infer<typeof resetSchema>>({
    resolver: zodResolver(resetSchema),
    defaultValues: { code: "", newPassword: "", confirmPassword: "" },
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

  async function onForgotSubmit(values: { identifier: string }) {
    setResetSubmitting(true)
    try {
      await forgotPassword(values.identifier)
      setSavedIdentifier(values.identifier)
      setResetStep("code")
      toast.success("Verification code sent to your registered phone/email")
    } catch (error) {
      toast.error(describeAuthError(error))
    } finally {
      setResetSubmitting(false)
    }
  }

  async function onResetSubmit(values: z.infer<typeof resetSchema>) {
    setResetSubmitting(true)
    try {
      await resetPassword(savedIdentifier, values.code, values.newPassword)
      toast.success("Password reset successfully. You can now sign in.")
      setForgotOpen(false)
      setResetStep("identifier")
      forgotForm.reset()
      resetForm.reset()
    } catch (error) {
      toast.error(describeAuthError(error))
    } finally {
      setResetSubmitting(false)
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
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Password</Label>
                <button
                  type="button"
                  onClick={() => {
                    setForgotOpen(true)
                    setResetStep("identifier")
                  }}
                  className="text-xs text-primary hover:underline"
                >
                  Forgot password?
                </button>
              </div>
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

      {/* Forgot / Reset Password Dialog */}
      <Dialog open={forgotOpen} onOpenChange={setForgotOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 font-heading">
              <KeyRound className="size-5" />
              {resetStep === "identifier" ? "Forgot Password" : "Reset Password"}
            </DialogTitle>
            <DialogDescription>
              {resetStep === "identifier"
                ? "Enter your email address or phone number to receive a verification code."
                : `Enter the 6-digit code sent for ${savedIdentifier} and choose a new password.`}
            </DialogDescription>
          </DialogHeader>

          {resetStep === "identifier" ? (
            <form onSubmit={forgotForm.handleSubmit(onForgotSubmit)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="forgot-identifier">Email or Phone Number</Label>
                <Input
                  id="forgot-identifier"
                  placeholder="admin@obs.local or +855000000000"
                  {...forgotForm.register("identifier")}
                />
                {forgotForm.formState.errors.identifier && (
                  <p className="text-xs text-destructive">
                    {forgotForm.formState.errors.identifier.message}
                  </p>
                )}
              </div>
              <DialogFooter className="pt-2">
                <Button type="button" variant="outline" onClick={() => setForgotOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={resetSubmitting}>
                  {resetSubmitting && <Loader2 className="mr-2 size-4 animate-spin" />}
                  Send Code
                </Button>
              </DialogFooter>
            </form>
          ) : (
            <form onSubmit={resetForm.handleSubmit(onResetSubmit)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="reset-code">6-Digit Code</Label>
                <Input
                  id="reset-code"
                  placeholder="123456"
                  maxLength={6}
                  className="font-mono text-center tracking-widest"
                  {...resetForm.register("code")}
                />
                {resetForm.formState.errors.code && (
                  <p className="text-xs text-destructive">
                    {resetForm.formState.errors.code.message}
                  </p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="reset-newPassword">New Password</Label>
                <Input
                  id="reset-newPassword"
                  type="password"
                  placeholder="••••••••"
                  {...resetForm.register("newPassword")}
                />
                {resetForm.formState.errors.newPassword && (
                  <p className="text-xs text-destructive">
                    {resetForm.formState.errors.newPassword.message}
                  </p>
                )}
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="reset-confirmPassword">Confirm Password</Label>
                <Input
                  id="reset-confirmPassword"
                  type="password"
                  placeholder="••••••••"
                  {...resetForm.register("confirmPassword")}
                />
                {resetForm.formState.errors.confirmPassword && (
                  <p className="text-xs text-destructive">
                    {resetForm.formState.errors.confirmPassword.message}
                  </p>
                )}
              </div>
              <DialogFooter className="pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setResetStep("identifier")}
                >
                  Back
                </Button>
                <Button type="submit" disabled={resetSubmitting}>
                  {resetSubmitting && <Loader2 className="mr-2 size-4 animate-spin" />}
                  Reset Password
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
