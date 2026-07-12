import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export type TicketType = "CONTENT_REPORT" | "ACCOUNT_APPEAL" | "FEATURE_FEEDBACK" | "SUGGESTION";
export type TicketStatus = "PENDING" | "RESOLVED" | "REJECTED" | "CANCELLED";
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
  uid?: string;
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
  userUid: string;
  banned: boolean;
  restrictions: ActiveRestriction[];
}

export interface TicketResponse {
  ticketId?: string;
  ticketType?: TicketType;
  status?: TicketStatus;
  submitter?: UserBriefWithStats;
  auditor?: {
    uid?: string;
    displayName?: string;
    avatar?: string | AvatarInfo | null;
    level?: number;
    userType?: string;
  } | null;
  targetUser?: UserBriefWithStats;
  targetId?: string;
  targetType?: string;
  content?: string;
  rejectType?: RejectType;
  auditNote?: string | null;
  replyContent?: string | null;
  evidenceResourceUuids?: string[];
  evidenceUrls?: string[];
  originalPenalty?: any;
  currentBans?: BanStatusInfo;
  timeline?: TicketTimeline;
  createdAt?: ISOString;
  auditAt?: string | null;
  updatedAt?: ISOString;
}

export interface ListTicketParams {
  page?: number;
  size?: number;
  ticketTypes?: string;
  ticketType?: TicketType;
  status?: TicketStatus;
  targetId?: string;
}

export interface TicketReviewRequest {
  action: ReviewAction;
  rejectType?: RejectType;
  auditNote?: string;
  penaltyType?: PenaltyType;
  penaltyDurationHours?: number;
  banReasonType?: BanReasonType;
  replyContent?: string;
}

export interface TicketStats {
  reportCount: string;
  appealCount: string;
  feedbackCount: string;
  suggestionCount: string;
}

export const listTickets = (params: ListTicketParams = {}): PromiseResBody<Page<TicketResponse>> => {
  return request.get<Page<TicketResponse>>("/tickets", { params });
};

export const getTicketDetail = (id: number): PromiseResBody<TicketResponse> => {
  return request.get<TicketResponse>(`/tickets/${id}`);
};

export const getTicketStats = (): PromiseResBody<TicketStats> => {
  return request.get<TicketStats>("/tickets/stats");
};

export const reviewTicket = (id: number, data: TicketReviewRequest): PromiseResBody<null> => {
  return request.post<null>(`/tickets/${id}/review`, data);
};

export const restoreTicket = (id: number): PromiseResBody<null> => {
  return request.post<null>(`/tickets/${id}/restore`);
};
