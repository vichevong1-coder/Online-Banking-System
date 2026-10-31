import { useCallback, useEffect, useState } from "react"
import { Check, Loader2, X } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { DataTable, type Column } from "@/components/ui/data-table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/features/auth/auth-context"
import { formatDate } from "@/features/customers/api"
import {
  approveKyc,
  getKycApplication,
  getKycDocumentImage,
  getPendingKyc,
  KYC_DOCUMENT_LABELS,
  rejectKyc,
  type KycApplication,
  type KycDocumentType,
  type PendingKycSummary,
} from "@/features/kyc/api"

const PAGE_SIZE = 20
const MAX_REASON_LENGTH = 500

// Staff review of customer sign-ups: look at the NID photos and selfie, then approve or reject.
// Open to ADMIN and TELLER (see permissions.ts).
export function KycPage() {
  const { accessToken } = useAuth()

  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<PendingKycSummary[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  const [selected, setSelected] = useState<PendingKycSummary | null>(null)

  useEffect(() => {
    if (!accessToken) return
    let cancelled = false

    setLoading(true)
    setError(null)
    getPendingKyc(accessToken, { page, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return
        setRows(result.content)
        setTotalElements(result.totalElements)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError("Couldn't load pending applications. Please try again.")
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [accessToken, page, reloadKey])

  const handleReviewed = useCallback(() => {
    setSelected(null)
    // Leave an emptied last page rather than showing a blank table.
    setPage((current) => (rows.length === 1 && current > 0 ? current - 1 : current))
    setReloadKey((key) => key + 1)
  }, [rows.length])

  const columns: Column<PendingKycSummary>[] = [
    {
      key: "name",
      header: "Name",
      render: (row) => (
        <span className="font-medium">
          {row.firstName} {row.lastName}
        </span>
      ),
    },
    { key: "phone", header: "Phone", render: (row) => row.phone },
    {
      key: "documents",
      header: "Documents",
      render: (row) => (
        <span className={row.documentCount < 3 ? "text-destructive" : "text-muted-foreground"}>
          {row.documentCount} of 3
        </span>
      ),
    },
    {
      key: "registeredAt",
      header: "Registered",
      render: (row) => <span className="text-muted-foreground">{formatDate(row.registeredAt)}</span>,
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="font-heading text-lg font-semibold">KYC Review</h2>
        <p className="text-sm text-muted-foreground">
          {loading
            ? "Loading…"
            : `${totalElements} application${totalElements === 1 ? "" : "s"} waiting for review`}
        </p>
      </div>

      {error ? (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      ) : (
        <DataTable
          columns={columns}
          rows={rows}
          getRowId={(row) => row.userId}
          loading={loading}
          emptyMessage="No applications are waiting for review."
          onRowClick={setSelected}
          activeRowId={selected?.userId ?? null}
        />
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}

      {selected && accessToken && (
        <KycReviewDialog
          key={selected.userId}
          accessToken={accessToken}
          summary={selected}
          onClose={() => setSelected(null)}
          onReviewed={handleReviewed}
        />
      )}
    </div>
  )
}

type ImageState = { status: "loading" } | { status: "ready"; url: string } | { status: "error" }

function KycReviewDialog({
  accessToken,
  summary,
  onClose,
  onReviewed,
}: {
  accessToken: string
  summary: PendingKycSummary
  onClose: () => void
  onReviewed: () => void
}) {
  const [application, setApplication] = useState<KycApplication | null>(null)
  const [loadError, setLoadError] = useState(false)
  const [images, setImages] = useState<Partial<Record<KycDocumentType, ImageState>>>({})

  const [rejecting, setRejecting] = useState(false)
  const [reason, setReason] = useState("")
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    const objectUrls: string[] = []

    getKycApplication(accessToken, summary.userId)
      .then((result) => {
        if (cancelled) return
        setApplication(result)
        setImages(Object.fromEntries(result.documents.map((doc) => [doc.type, { status: "loading" }])))

        result.documents.forEach((doc) => {
          getKycDocumentImage(accessToken, summary.userId, doc.type)
            .then((blob) => {
              const url = URL.createObjectURL(blob)
              if (cancelled) {
                URL.revokeObjectURL(url)
                return
              }
              objectUrls.push(url)
              setImages((current) => ({ ...current, [doc.type]: { status: "ready", url } }))
            })
            .catch(() => {
              if (!cancelled) setImages((current) => ({ ...current, [doc.type]: { status: "error" } }))
            })
        })
      })
      .catch(() => {
        if (!cancelled) setLoadError(true)
      })

    // Object URLs hold the decoded ID photos in memory, so release them when the dialog closes.
    return () => {
      cancelled = true
      objectUrls.forEach((url) => URL.revokeObjectURL(url))
    }
  }, [accessToken, summary.userId])

  const allDocumentsPresent = application ? application.documents.length === 3 : false
  const trimmedReason = reason.trim()

  async function approve() {
    setSubmitting(true)
    try {
      await approveKyc(accessToken, summary.userId)
      toast.success(`${summary.firstName} ${summary.lastName} was approved.`)
      onReviewed()
    } catch {
      toast.error("Couldn't approve this application.")
      setSubmitting(false)
    }
  }

  async function reject() {
    if (!trimmedReason) return
    setSubmitting(true)
    try {
      await rejectKyc(accessToken, summary.userId, trimmedReason)
      toast.success(`${summary.firstName} ${summary.lastName} was rejected.`)
      onReviewed()
    } catch {
      toast.error("Couldn't reject this application.")
      setSubmitting(false)
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {summary.firstName} {summary.lastName}
          </DialogTitle>
          <DialogDescription>
            {summary.phone} · registered {formatDate(summary.registeredAt)}
          </DialogDescription>
        </DialogHeader>

        {loadError ? (
          <div className="rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
            Couldn't load this application.
          </div>
        ) : !application ? (
          <div className="flex justify-center py-8">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <Detail label="National ID number" value={application.nidNumber} />
              <Detail label="ID expiry" value={application.nidExpiryDate ? formatDate(application.nidExpiryDate) : null} />
              <Detail label="Date of birth" value={application.dateOfBirth ? formatDate(application.dateOfBirth) : null} />
              <Detail label="Gender" value={application.gender} />
            </dl>

            <div className="grid gap-3 sm:grid-cols-3">
              {(["NID_FRONT", "NID_BACK", "SELFIE"] as KycDocumentType[]).map((type) => (
                <DocumentPreview key={type} type={type} state={images[type]} />
              ))}
            </div>

            {!allDocumentsPresent && (
              <p className="text-sm text-destructive">
                This application has {application.documents.length} of 3 documents. Reject it so the customer
                can register again.
              </p>
            )}

            {rejecting && (
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="kyc-reject-reason">Reason shown to the customer</Label>
                <textarea
                  id="kyc-reject-reason"
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  maxLength={MAX_REASON_LENGTH}
                  rows={3}
                  placeholder="For example: the ID photo is blurry, please retake it."
                  className="w-full rounded-md border bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
                />
                <span className="text-xs text-muted-foreground">
                  {reason.length}/{MAX_REASON_LENGTH}
                </span>
              </div>
            )}

            <DialogFooter>
              {rejecting ? (
                <>
                  <Button variant="outline" disabled={submitting} onClick={() => setRejecting(false)}>
                    Back
                  </Button>
                  <Button variant="destructive" disabled={submitting || !trimmedReason} onClick={reject}>
                    <X className="mr-2 size-4" />
                    Confirm rejection
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="outline" disabled={submitting} onClick={() => setRejecting(true)}>
                    <X className="mr-2 size-4" />
                    Reject
                  </Button>
                  <Button disabled={submitting || !allDocumentsPresent} onClick={approve}>
                    <Check className="mr-2 size-4" />
                    Approve
                  </Button>
                </>
              )}
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

function Detail({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value ?? "Not provided"}</dd>
    </div>
  )
}

function DocumentPreview({ type, state }: { type: KycDocumentType; state: ImageState | undefined }) {
  const label = KYC_DOCUMENT_LABELS[type]
  return (
    <figure className="flex flex-col gap-1.5">
      <div className="flex aspect-[4/3] items-center justify-center overflow-hidden rounded-md border bg-muted">
        {!state ? (
          <span className="px-2 text-center text-xs text-destructive">Not uploaded</span>
        ) : state.status === "loading" ? (
          <Loader2 className="size-5 animate-spin text-muted-foreground" />
        ) : state.status === "error" ? (
          <span className="px-2 text-center text-xs text-destructive">Couldn't load image</span>
        ) : (
          <a href={state.url} target="_blank" rel="noreferrer" className="block size-full">
            <img src={state.url} alt={label} className="size-full object-contain" />
          </a>
        )}
      </div>
      <figcaption className="text-xs text-muted-foreground">{label}</figcaption>
    </figure>
  )
}
