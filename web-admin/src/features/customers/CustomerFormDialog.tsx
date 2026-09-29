import { useState, useEffect } from "react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { createCustomer, updateCustomer, type CustomerDetail, type CreateCustomerPayload, type UpdateCustomerPayload } from "./api"

interface CustomerFormDialogProps {
  accessToken: string
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: (customer: CustomerDetail, isEdit: boolean) => void
  editingCustomer?: CustomerDetail | null
}

export function CustomerFormDialog({
  accessToken,
  open,
  onOpenChange,
  onSuccess,
  editingCustomer,
}: CustomerFormDialogProps) {
  const [loading, setLoading] = useState(false)
  const isEdit = !!editingCustomer

  const [formData, setFormData] = useState({
    firstName: editingCustomer?.firstName || "",
    lastName: editingCustomer?.lastName || "",
    phone: editingCustomer?.phone || "",
    gender: editingCustomer?.gender || "MALE",
  })

  // Update form data when dialog opens
  useEffect(() => {
    if (open) {
      setFormData({
        firstName: editingCustomer?.firstName || "",
        lastName: editingCustomer?.lastName || "",
        phone: editingCustomer?.phone || "",
        gender: editingCustomer?.gender || "MALE",
      })
    }
  }, [open, editingCustomer])
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      if (isEdit) {
        const payload: UpdateCustomerPayload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          phone: formData.phone,
          gender: formData.gender as "MALE" | "FEMALE",
        }
        const updated = await updateCustomer(accessToken, editingCustomer!.id, payload)
        toast.success("Customer updated successfully.")
        onSuccess(updated, true)
      } else {
        const payload: CreateCustomerPayload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          phone: formData.phone,
          gender: formData.gender as "MALE" | "FEMALE",
          password: "password123", // default password
        }
        const created = await createCustomer(accessToken, payload)
        toast.success("Customer created successfully.")
        onSuccess(created, false)
      }
      onOpenChange(false)
    } catch (error) {
      toast.error(isEdit ? "Failed to update customer." : "Failed to create customer.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEdit ? "Edit Customer" : "New Customer"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Update customer details below."
                : "Create a new customer account. Default password will be password123."}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="firstName" className="text-right">
                First Name
              </Label>
              <Input
                id="firstName"
                value={formData.firstName}
                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                className="col-span-3"
                required
              />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="lastName" className="text-right">
                Last Name
              </Label>
              <Input
                id="lastName"
                value={formData.lastName}
                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                className="col-span-3"
                required
              />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="phone" className="text-right">
                Phone
              </Label>
              <Input
                id="phone"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                className="col-span-3"
                placeholder="+855..."
                required
              />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="gender" className="text-right">
                Gender
              </Label>
              <select
                id="gender"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value as "MALE" | "FEMALE" | "OTHER" })}
                className="col-span-3 flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Saving..." : isEdit ? "Update" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
