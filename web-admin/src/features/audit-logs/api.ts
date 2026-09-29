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
  if (params.startDate) query.set("startDate", params.startDate)
  if (params.endDate) query.set("endDate", params.endDate)

  const response = await fetch(`/api/admin/audit-logs?${query.toString()}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  })

  if (!response.ok) {
    throw new Error("Failed to fetch audit logs")
  }
  return response.json()
}
