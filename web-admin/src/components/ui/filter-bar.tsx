import * as React from "react"
import { X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

export interface FilterBarProps extends React.HTMLAttributes<HTMLDivElement> {
  onReset?: () => void
  isFiltered?: boolean
  children?: React.ReactNode
}

export function FilterBar({
  className,
  onReset,
  isFiltered = false,
  children,
  ...props
}: FilterBarProps) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-end gap-3 rounded-lg border bg-card p-3 text-card-foreground shadow-xs",
        className,
      )}
      {...props}
    >
      {children}
      {isFiltered && onReset && (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onReset}
          className="h-8 px-2.5 text-xs font-normal text-muted-foreground hover:text-foreground"
        >
          <X className="mr-1 size-3.5" />
          Reset filters
        </Button>
      )}
    </div>
  )
}

export interface FilterItemProps extends React.HTMLAttributes<HTMLDivElement> {
  label?: string
  htmlFor?: string
  children: React.ReactNode
}

export function FilterItem({ label, htmlFor, className, children, ...props }: FilterItemProps) {
  return (
    <div className={cn("flex flex-col gap-1", className)} {...props}>
      {label && (
        <Label htmlFor={htmlFor} className="text-xs font-medium text-muted-foreground">
          {label}
        </Label>
      )}
      {children}
    </div>
  )
}

export interface FilterSelectProps extends React.ComponentProps<"select"> {
  className?: string
}

export function FilterSelect({ className, children, ...props }: FilterSelectProps) {
  return (
    <select
      className={cn(
        "h-8 min-w-32 rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 dark:bg-input/30",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
}
