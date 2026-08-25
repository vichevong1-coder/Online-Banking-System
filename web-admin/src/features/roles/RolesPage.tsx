import { useCallback, useEffect, useState } from "react"
import { Plus, Shield } from "lucide-react"
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
import { StatusBadge } from "@/features/customers/StatusBadge"
import { formatDate } from "@/features/customers/api"
import {
  createStaff,
  getStaff,
  updateStaffRole,
  type CreateStaffPayload,
  type Role,
  type StaffMember,
} from "@/features/admin/api"

// Mirrors the backend's CreateStaffRequest phone constraint: optional leading "+",
// then a digit followed by 6-28 more digits, spaces, hyphens or parentheses —
// capping the total at the 30 characters the phone column holds.
const PHONE_PATTERN = /^\+?[0-9][0-9 ()-]{6,28}$/

export function RolesPage() {
  const { accessToken } = useAuth()
  const [staffList, setStaffList] = useState<StaffMember[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Create Staff Modal State
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [formData, setFormData] = useState<CreateStaffPayload>({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    role: "ADMIN",
    password: "",
  })

  // Edit Role Modal State
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null)
  const [editRole, setEditRole] = useState<Role>("ADMIN")
  const [updatingRole, setUpdatingRole] = useState(false)

  const fetchStaff = useCallback(async () => {
    if (!accessToken) return
    setLoading(true)
    setError(null)
    try {
      const data = await getStaff(accessToken)
      setStaffList(data)
    } catch {
      setError("Couldn't load staff members. Please try again.")
    } finally {
      setLoading(false)
    }
  }, [accessToken])

  useEffect(() => {
    fetchStaff()
  }, [fetchStaff])

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!accessToken) return

    if (!formData.firstName || !formData.lastName || !formData.email || !formData.phone || !formData.password) {
      toast.error("Please fill in all required fields.")
      return
    }

    if (!PHONE_PATTERN.test(formData.phone)) {
      toast.error("Enter a valid phone number.")
      return
    }

    if (formData.password.length < 8) {
      toast.error("Password must be at least 8 characters.")
      return
    }

    setCreating(true)
    try {
      const created = await createStaff(accessToken, formData)
      setStaffList((prev) => [...prev, created])
      toast.success(`Staff account created for ${created.firstName} ${created.lastName}.`)
      setCreateDialogOpen(false)
      setFormData({
        firstName: "",
        lastName: "",
        email: "",
        phone: "",
        role: "ADMIN",
        password: "",
      })
    } catch (err: any) {
      toast.error(err?.code ? `Failed: ${err.code}` : "Couldn't create staff member.")
    } finally {
      setCreating(false)
    }
  }

  const handleUpdateRole = async () => {
    if (!accessToken || !editingStaff) return
    setUpdatingRole(true)
    try {
      const updated = await updateStaffRole(accessToken, editingStaff.id, editRole)
      setStaffList((prev) => prev.map((s) => (s.id === updated.id ? updated : s)))
      toast.success(`Role updated to ${updated.role} for ${updated.firstName} ${updated.lastName}.`)
      setEditingStaff(null)
    } catch {
      toast.error("Couldn't update staff role.")
    } finally {
      setUpdatingRole(false)
    }
  }

  const columns: Column<StaffMember>[] = [
    {
      key: "name",
      header: "Name",
      render: (staff) => (
        <span className="font-medium">
          {staff.firstName} {staff.lastName}
        </span>
      ),
    },
    { key: "email", header: "Email", render: (staff) => staff.email },
    { key: "phone", header: "Phone", render: (staff) => staff.phone },
    {
      key: "role",
      header: "Role",
      render: (staff) => (
        <Badge variant={staff.role === "ADMIN" ? "default" : "secondary"}>
          <Shield className="size-3 mr-1" />
          {staff.role}
        </Badge>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (staff) => <StatusBadge status={staff.status} />,
    },
    {
      key: "createdAt",
      header: "Created At",
      render: (staff) => <span className="text-muted-foreground">{formatDate(staff.createdAt)}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "text-right",
      render: (staff) => (
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            setEditingStaff(staff)
            setEditRole(staff.role)
          }}
        >
          Change Role
        </Button>
      ),
    },
  ]

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">Roles & Staff</h2>
          <p className="text-sm text-muted-foreground">Manage administrative accounts and permission levels</p>
        </div>

        <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm">
              <Plus className="size-4 mr-1" />
              Add Staff Member
            </Button>
          </DialogTrigger>
          <DialogContent>
            <form onSubmit={handleCreateStaff}>
              <DialogHeader>
                <DialogTitle>Add Staff Member</DialogTitle>
                <DialogDescription>Create a new administrative staff account.</DialogDescription>
              </DialogHeader>

              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="firstName">First Name</Label>
                    <Input
                      id="firstName"
                      value={formData.firstName}
                      onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                      placeholder="e.g. Sothea"
                      maxLength={255}
                      required
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="lastName">Last Name</Label>
                    <Input
                      id="lastName"
                      value={formData.lastName}
                      onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                      placeholder="e.g. Chan"
                      maxLength={255}
                      required
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    placeholder="staff@bank.com"
                    maxLength={255}
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="phone">Phone Number</Label>
                  <Input
                    id="phone"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    placeholder="+85512345678"
                    maxLength={30}
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="role">Role</Label>
                  <select
                    id="role"
                    className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    value={formData.role}
                    onChange={(e) => setFormData({ ...formData, role: e.target.value as Role })}
                  >
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="password">Temporary Password</Label>
                  <Input
                    id="password"
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    placeholder="At least 8 characters"
                    minLength={8}
                    maxLength={100}
                    required
                  />
                </div>
              </div>

              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setCreateDialogOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={creating}>
                  {creating ? "Creating…" : "Create Staff Account"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      <DataTable
        columns={columns}
        rows={staffList}
        getRowId={(staff) => staff.id}
        loading={loading}
        emptyMessage="No staff accounts found."
      />

      {/* Edit Role Dialog */}
      <Dialog open={editingStaff !== null} onOpenChange={(open) => !open && setEditingStaff(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Update Staff Role</DialogTitle>
            <DialogDescription>
              Modify role and permissions for {editingStaff?.firstName} {editingStaff?.lastName}.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4 space-y-3">
            <Label htmlFor="edit-role">Assigned Role</Label>
            <select
              id="edit-role"
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              value={editRole}
              onChange={(e) => setEditRole(e.target.value as Role)}
            >
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setEditingStaff(null)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateRole} disabled={updatingRole}>
              {updatingRole ? "Updating…" : "Save Role"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
