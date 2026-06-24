import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type TicketType = "CONTENT_REPORT" | "ACCOUNT_APPEAL" | "FEATURE_FEEDBACK" | "SUGGESTION";
export type TicketStatus = "PENDING" | "RESOLVED" | "REJECTED";
export type ReviewAction = "APPROVE" | "REJECT";
export type RejectType =
  | "NONE"
  | "INSUFFICIENT_EVIDENCE"
  | "NO_VIOLATION"
  | "DUPLICATE_REPORT"
  | "MALICIOUS_REPORT"
  | "BEYOND_SCOPE"
  | "FALSE_POSITIVE"
  | "APPEAL_ACCEPTED"
  | "OTHER";
export type PenaltyType =
  | "UNSPECIFIED"
  | "BAN_LOGIN"
  | "BAN_POST"
  | "BAN_COMMENT"
  | "BAN_UPLOAD"
  | "BAN_CHAT"
  | "BAN_CREATE"
  | "OTHER";
export type BanReasonType =
  | "UNSPECIFIED"
  | "VIOLATION_OF_GUIDELINES"
  | "INAPPROPRIATE_CONTENT"
  | "ADVERTISEMENT"
  | "VIOLENCE"
  | "SENSITIVE"
  | "CHEATING"
  | "IMPERSONATION"
  | "PRIVACY"
  | "TROLLING"
  | "OTHER";

export interface AvatarInfo {
  url?: string;
  expireAt?: string;
}

export interface UserBriefWithStats {
  uid?: number;
  nickname?: string;
  displayName?: string;
  avatar?: string | AvatarInfo | null;
  level?: number;
  userType?: "COMMON" | "ADMIN" | "BOT" | "MODERATOR" | "VIP";
  registrationDate?: string;
  postCount?: number;
  reportCount?: number;
  penaltyCount?: number;
  riskLevel?: "LOW" | "MEDIUM" | "HIGH";
}

export interface TicketTimeline {
  submittedAt?: string;
  reviewedAt?: string | null;
  status?: string;
}

export interface ActiveRestriction {
  permissionCode: string;
  permissionName: string;
  banReasonType: string | null;
  expiresAt: string | null;
  permanent: boolean;
  createdAt: string;
}

export interface BanStatusInfo {
  userUid: number;
  banned: boolean;
  restrictions: ActiveRestriction[];
}

export interface TicketResponse {
  /** API field: ticketId (not id) */
  ticketId?: number;
  ticketType?: TicketType;
  status?: TicketStatus;
  submitter?: UserBriefWithStats;
  auditor?: UserBriefWithStats | null;
  targetUserId?: number;
  targetUser?: UserBriefWithStats;
  targetId?: string | null;
  targetType?: string;
  content?: string;
  rejectType?: RejectType;
  auditNote?: string | null;
  replyContent?: string | null;
  evidenceResourceUuids?: string[];
  originalPenalty?: any;
  currentBans?: BanStatusInfo;
  timeline?: TicketTimeline;
  createdAt?: ISOString;
  auditAt?: string | null;
  updatedAt?: ISOString;

  /** Penalty fields used by appeals */
  penaltyType?: PenaltyType;
  banReasonType?: BanReasonType;

  /** Legacy/fallback aliases used in templates */
  id?: number;
  submitterId?: number;
  attachments?: string[];
  source?: string;
  sourceUrl?: string;
  relatedTitle?: string;
}

export interface ListTicketParams {
  page?: number;
  size?: number;
  /** 逗号分隔的工单类型列表，如 "SUGGESTION,FEATURE_FEEDBACK" */
  ticketTypes?: string;
  /** 兼容旧版：单类型（已废弃，建议使用 ticketTypes） */
  ticketType?: TicketType;
  status?: TicketStatus;
  targetId?: string;
}

export interface TicketReviewRequest {
  action: ReviewAction;
  rejectType?: RejectType;
  auditNote?: string;
  penaltyType?: PenaltyType;
  penaltyDurationHours?: string;
  banReasonType?: BanReasonType;
  replyContent?: string;
}

export const listTickets = (params: ListTicketParams = {}): PromiseResBody<Page<TicketResponse>> => {
  return request.get<Page<TicketResponse>>("/tickets", { params });
};

export const getTicketDetail = (id: number): PromiseResBody<TicketResponse> => {
  return request.get<TicketResponse>(`/tickets/${id}`);
};

export const getTicketStats = (): PromiseResBody<{
  reportCount: number;
  appealCount: number;
  feedbackCount: number;
  suggestionCount: number;
}> => {
  return request.get("/tickets/stats");
};

export const createTicket = (data: {
  ticketType: TicketType;
  type: string;
  reason: string;
  targetId?: string;
  targetType?: string;
  resourceUuids?: string[];
  penaltyType?: PenaltyType;
}): PromiseResBody<{ taskId: number }> => {
  return request.post("/tickets", data);
};

export const cancelTicket = (id: number): PromiseResBody<null> => {
  return request.post<null>(`/tickets/${id}/cancel`);
};

export const reviewTicket = (id: number, data: TicketReviewRequest): PromiseResBody<null> => {
  return request.post<null>(`/tickets/${id}/review`, data);
};


