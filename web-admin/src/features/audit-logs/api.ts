import { authFetch } from "@/lib/api-client"

export interface AuditLog {
  id: string
  actorId: string
  actorEmail: string
  actionType: string
  entityId: string | null
  entityType: string | null
  details: string | null
  ipAddress: string | null
  createdAt: string
}

export interface AuditLogListResponse {
  content: AuditLog[]
  totalElements: number
  totalPages: number
}

interface GetAuditLogsParams {
  page: number
  size: number
  actionType?: string
  startDate?: string
  endDate?: string
}

export async function getAuditLogs(
  token: string,
  params: GetAuditLogsParams
): Promise<AuditLogListResponse> {
  const query = new URLSearchParams()
  query.set("page", params.page.toString())
  query.set("size", params.size.toString())
  if (params.actionType) query.set("actionType", params.actionType)
  if (params.startDate) {
    const formatted = params.startDate.includes("T")
      ? params.startDate
      : `${params.startDate}T00:00:00Z`
    query.set("startDate", formatted)
  }
  if (params.endDate) {
    const formatted = params.endDate.includes("T")
      ? params.endDate
      : `${params.endDate}T23:59:59Z`
    query.set("endDate", formatted)
  }

  return authFetch<AuditLogListResponse>(
    `/admin/audit-logs?${query.toString()}`,
    token,
  )
}

