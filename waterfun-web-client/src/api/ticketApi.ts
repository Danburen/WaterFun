import request from "../utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"

export type TicketType = 'CONTENT_REPORT' | 'ACCOUNT_APPEAL' | 'FEATURE_FEEDBACK' | 'SUGGESTION'

export type TicketStatus = 'PENDING' | 'RESOLVED' | 'REJECTED' | 'CANCELLED'

export type ReportType = 'VIOLATION_OF_GUIDELINES' | 'INAPPROPRIATE_CONTENT' | 'ADVERTISEMENT' | 'VIOLENCE' | 'SENSITIVE' | 'CASCADE' | 'OTHER'

export type TargetType = 'DEFAULT' | 'USER_AVATAR' | 'POST' | 'POST_COVERAGE_IMAGE' | 'POST_CONTENT_IMAGE' | 'BANNER_IMAGE' | 'COMMENT' | 'USER_REPORT_ATTACHMENT' | 'MODERATION_IMAGE' | 'USER'

export type PenaltyType = 'UNSPECIFIED' | 'BAN_LOGIN' | 'BAN_POST' | 'BAN_COMMENT' | 'BAN_UPLOAD' | 'BAN_CHAT' | 'BAN_CREATE' | 'OTHER'

export type RejectType = 'NONE' | 'INSUFFICIENT_EVIDENCE' | 'NO_VIOLATION' | 'DUPLICATE_REPORT' | 'MALICIOUS_REPORT' | 'BEYOND_SCOPE' | 'FALSE_POSITIVE' | 'APPEAL_ACCEPTED' | 'OTHER'

export interface UserTicketListResponse {
  ticketId: string
  ticketType: TicketType
  status: TicketStatus
  content: string
  targetId: string
  targetType: TargetType
  createdAt: string
  updatedAt: string
  auditAt: string | null
  auditNote: string | null
  rejectType: RejectType | null
  evidenceCount: number
}

export interface UserTicketDetailResponse {
  ticketId: string
  ticketType: TicketType
  status: TicketStatus
  content: string
  targetId: string
  targetType: TargetType
  createdAt: string
  updatedAt: string
  auditAt: string | null
  auditNote: string | null
  rejectType: RejectType | null
  evidence: EvidenceItem[]
  timeline: Timeline | null
  replies: ReplyItem[]
}

export interface EvidenceItem {
  uuid: string
  url: string
  expireAt: string | null
}

export interface Timeline {
  submittedAt: string
  reviewedAt: string | null
  status: TicketStatus
}

export interface ReplyItem {
  id: string
  content: string
  senderName: string
  createdAt: string
}

export interface TicketStatsResponse {
  reportCount: number
  appealCount: number
  feedbackCount: number
  suggestionCount: number
}

export interface CreateUserReportReq {
  ticketType: TicketType
  type?: ReportType
  reason?: string
  targetId?: string
  targetType?: TargetType
  resourceUuids?: string[]
  penaltyType?: PenaltyType
}

export interface ReportResponse {
  taskId: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const fetchTicketList = (params: {
  ticketType?: TicketType
  status?: TicketStatus
  page?: number
  size?: number
}): PromiseResBody<PageResult<UserTicketListResponse>> => {
  return request.get('/tickets', { params: { ...params, page: params.page || 1, size: params.size || 10 } })
}

export const createTicket = (data: CreateUserReportReq): PromiseResBody<ReportResponse> => {
  return request.post('/tickets', data)
}

export const fetchTicketDetail = (id: string): PromiseResBody<UserTicketDetailResponse> => {
  return request.get(`/tickets/${id}`)
}

export const cancelTicket = (id: string): PromiseResBody<void> => {
  return request.post(`/tickets/${id}/cancel`)
}

export const fetchTicketStats = (): PromiseResBody<TicketStatsResponse> => {
  return request.get('/tickets/stats')
}
