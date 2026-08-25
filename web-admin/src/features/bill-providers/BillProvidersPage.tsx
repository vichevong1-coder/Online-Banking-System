import { useCallback, useEffect, useMemo, useState } from "react"
import { Plus, Search, Trash2 } from "lucide-react"
import { toast } from "sonner"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { DataTable, type Column } from "@/components/ui/data-table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/features/auth/auth-context"
import { formatDate } from "@/features/customers/api"
import {
  createBillProvider,
  deleteBillProvider,
  getBillProviders,
  updateBillProvider,
} from "@/features/bill-providers/api"
import type {
  BillCategory,
  BillProvider,
  CreateBillProviderPayload,
  UpdateBillProviderPayload,
} from "@/features/bill-providers/types"

const CATEGORIES: BillCategory[] = [
  "ELECTRICITY",
  "WATER",
  "INTERNET",
  "MOBILE_TOPUP",
  "OTHER",
]

export function BillProvidersPage() {
  const { accessToken } = useAuth()
  const [providers, setProviders] = useState<BillProvider[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState("")
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL")

  // Create Modal State
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [createForm, setCreateForm] = useState<CreateBillProviderPayload>({
    name: "",
    category: "ELECTRICITY",
    accountNumberPattern: "",
    active: true,
  })

  // Edit Modal State
  const [editingProvider, setEditingProvider] = useState<BillProvider | null>(null)
  const [editForm, setEditForm] = useState<UpdateBillProviderPayload>({})
  const [updating, setUpdating] = useState(false)

  // Delete Modal State
  const [deletingProvider, setDeletingProvider] = useState<BillProvider | null>(null)
  const [deleting, setDeleting] = useState(false)

  const fetchProviders = useCallback(async () => {
    if (!accessToken) return
    setLoading(true)
    setError(null)
    try {
      const data = await getBillProviders(accessToken)
      setProviders(data)
    } catch {
      setError("Couldn't load bill providers. Please try again.")
    } finally {
      setLoading(false)
    }
  }, [accessToken])

  useEffect(() => {
    fetchProviders()
  }, [fetchProviders])

  const filteredProviders = useMemo(() => {
    return providers.filter((p) => {
      const matchesSearch =
        search === "" ||
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.category.toLowerCase().includes(search.toLowerCase())
      const matchesCategory = selectedCategory === "ALL" || p.category === selectedCategory
      return matchesSearch && matchesCategory
    })
  }, [providers, search, selectedCategory])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accessToken) return
    if (!createForm.name.trim()) {
      toast.error("Provider name is required.")
      return
    }

    setCreating(true)
    try {
      const created = await createBillProvider(accessToken, {
        name: createForm.name.trim(),
        category: createForm.category,
        accountNumberPattern: createForm.accountNumberPattern?.trim() || undefined,
        active: createForm.active ?? true,
      })
      setProviders((prev) => [...prev, created])
      toast.success(`Provider "${created.name}" created successfully.`)
      setCreateDialogOpen(false)
      setCreateForm({
        name: "",
        category: "ELECTRICITY",
        accountNumberPattern: "",
        active: true,
      })
    } catch {
      toast.error("Failed to create bill provider.")
    } finally {
      setCreating(false)
    }
  }

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accessToken || !editingProvider) return

    setUpdating(true)
    try {
      const updated = await updateBillProvider(accessToken, editingProvider.id, {
        name: editForm.name?.trim() || undefined,
        category: editForm.category,
        accountNumberPattern: editForm.accountNumberPattern?.trim() || undefined,
        active: editForm.active,
      })
      setProviders((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
      toast.success(`Provider "${updated.name}" updated successfully.`)
      setEditingProvider(null)
    } catch {
      toast.error("Failed to update bill provider.")
    } finally {
      setUpdating(false)
    }
  }

  const handleDelete = async () => {
    if (!accessToken || !deletingProvider) return

    setDeleting(true)
    try {
      await deleteBillProvider(accessToken, deletingProvider.id)
      setProviders((prev) => prev.filter((p) => p.id !== deletingProvider.id))
      toast.success(`Provider "${deletingProvider.name}" removed.`)
      setDeletingProvider(null)
    } catch {
      toast.error("Failed to delete bill provider.")
    } finally {
      setDeleting(false)
    }
  }

  const columns: Column<BillProvider>[] = [
    {
      key: "name",
      header: "Provider Name",
      render: (p) => <span className="font-medium text-foreground">{p.name}</span>,
    },
    {
      key: "category",
      header: "Category",
      render: (p) => (
        <Badge variant="outline" className="font-mono text-xs">
          {p.category}
        </Badge>
      ),
    },
    {
      key: "pattern",
      header: "Account Pattern",
      render: (p) => (
        <span className="text-xs text-muted-foreground font-mono">
          {p.accountNumberPattern || "—"}
        </span>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (p) => (
        <Badge variant={p.active ? "default" : "secondary"}>
          {p.active ? "Active" : "Inactive"}
        </Badge>
      ),
    },
    {
      key: "createdAt",
      header: "Created",
      render: (p) => <span className="text-muted-foreground">{formatDate(p.createdAt)}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "text-right",
      render: (p) => (
        <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setEditingProvider(p)
              setEditForm({
                name: p.name,
                category: p.category,
                accountNumberPattern: p.accountNumberPattern || "",
                active: p.active,
              })
            }}
          >
            Edit
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-destructive hover:bg-destructive/10 hover:text-destructive"
            onClick={() => setDeletingProvider(p)}
          >
            <Trash2 className="size-4" />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">Bill Providers</h2>
          <p className="text-sm text-muted-foreground">Manage billers and account number formats</p>
        </div>

        <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm">
              <Plus className="size-4 mr-1" />
              Add Provider
            </Button>
          </DialogTrigger>
          <DialogContent>
            <form onSubmit={handleCreate}>
              <DialogHeader>
                <DialogTitle>Add Bill Provider</DialogTitle>
                <DialogDescription>Register a new bill provider for utility payments.</DialogDescription>
              </DialogHeader>

              <div className="grid gap-4 py-4">
                <div className="space-y-1.5">
                  <Label htmlFor="create-name">Provider Name</Label>
                  <Input
                    id="create-name"
                    value={createForm.name}
                    onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                    placeholder="e.g. EDC, PPWSA, Ezecom"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="create-category">Category</Label>
                  <select
                    id="create-category"
                    className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    value={createForm.category}
                    onChange={(e) =>
                      setCreateForm({ ...createForm, category: e.target.value as BillCategory })
                    }
                  >
                    {CATEGORIES.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="create-pattern">Account Number Pattern (Regex)</Label>
                  <Input
                    id="create-pattern"
                    value={createForm.accountNumberPattern}
                    onChange={(e) =>
                      setCreateForm({ ...createForm, accountNumberPattern: e.target.value })
                    }
                    placeholder="e.g. ^[0-9]{8,12}$"
                  />
                </div>

                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="create-active"
                    checked={createForm.active}
                    onChange={(e) => setCreateForm({ ...createForm, active: e.target.checked })}
                    className="size-4 rounded border-gray-300"
                  />
                  <Label htmlFor="create-active" className="cursor-pointer">
                    Active for customer payments
                  </Label>
                </div>
              </div>

              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setCreateDialogOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={creating}>
                  {creating ? "Saving…" : "Save Provider"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative w-full max-w-xs">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search provider or category"
            className="pl-8"
          />
        </div>

        <select
          className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
        >
          <option value="ALL">All Categories</option>
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <DataTable
        columns={columns}
        rows={filteredProviders}
        getRowId={(p) => p.id}
        loading={loading}
        emptyMessage={
          search || selectedCategory !== "ALL"
            ? "No bill providers match your filter."
            : "No bill providers registered yet."
        }
      />

      {/* Edit Provider Modal */}
      <Dialog
        open={editingProvider !== null}
        onOpenChange={(open) => !open && setEditingProvider(null)}
      >
        <DialogContent>
          <form onSubmit={handleUpdate}>
            <DialogHeader>
              <DialogTitle>Edit Bill Provider</DialogTitle>
              <DialogDescription>Update bill provider configuration.</DialogDescription>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              <div className="space-y-1.5">
                <Label htmlFor="edit-name">Provider Name</Label>
                <Input
                  id="edit-name"
                  value={editForm.name || ""}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="edit-category">Category</Label>
                <select
                  id="edit-category"
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                  value={editForm.category || "ELECTRICITY"}
                  onChange={(e) =>
                    setEditForm({ ...editForm, category: e.target.value as BillCategory })
                  }
                >
                  {CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="edit-pattern">Account Number Pattern (Regex)</Label>
                <Input
                  id="edit-pattern"
                  value={editForm.accountNumberPattern || ""}
                  onChange={(e) =>
                    setEditForm({ ...editForm, accountNumberPattern: e.target.value })
                  }
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="edit-active"
                  checked={editForm.active ?? true}
                  onChange={(e) => setEditForm({ ...editForm, active: e.target.checked })}
                  className="size-4 rounded border-gray-300"
                />
                <Label htmlFor="edit-active" className="cursor-pointer">
                  Active for customer payments
                </Label>
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setEditingProvider(null)}>
                Cancel
              </Button>
              <Button type="submit" disabled={updating}>
                {updating ? "Saving…" : "Save Changes"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deletingProvider !== null}
        onOpenChange={(open) => !open && setDeletingProvider(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Bill Provider</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{deletingProvider?.name}"? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeletingProvider(null)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? "Deleting…" : "Delete Provider"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
