import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import type { UserBrief } from "~/api/dashboard";
import request from "~/utils/axiosRequest";

export type TargetType =
  | "DEFAULT"
  | "USER_AVATAR"
  | "POST"
  | "POST_COVERAGE_IMAGE"
  | "POST_CONTENT_IMAGE"
  | "BANNER_IMAGE"
  | "COMMENT"
  | "USER_REPORT_ATTACHMENT"
  | "MODERATION_IMAGE"
  | "USER";

export type ModerateRejectType =
  | "VIOLATION_OF_GUIDELINES"
  | "INAPPROPRIATE_CONTENT"
  | "ADVERTISEMENT"
  | "VIOLENCE"
  | "SENSITIVE"
  | "CASCADE"
  | "OTHER";

export type AuditStatus = "PENDING" | "APPROVED" | "REJECTED" | "SUSPECT" | "CANCELED";

export interface PostAuditPayload {
  title?: string;
  subTitle?: string;
  content?: string;
  summary?: string;
  authorUid?: string;
  coverageResUuid?: string;
  categoryId?: number;
  tagIds?: number[];
  newTagNames?: string[];
  coverResPresignedUrl?: CloudResPresignedUrlResp;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
}

export interface FileMeta {
  etag?: string;
}

export interface FileProbeResult {
  size?: string;
  mimeType?: string;
  meta?: FileMeta;
}

export interface CloudResPresignedUrlResp {
  url?: string;
  expireAt?: ISOString;
}

export interface ModerationResourceRes {
  taskId?: string;
  resourceUuid?: string;
  resourceKey?: string;
  status?: AuditStatus;
  auditAt?: ISOString;
  auditorId?: number;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  fileProbeResult?: FileProbeResult | null;
  presignedUrl?: CloudResPresignedUrlResp | null;
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export interface BatchModerateRequest {
  auditTaskIds: string[];
}

export interface BatchModerateRejectRequest {
  auditTaskIds: string[];
  rejectType: ModerateRejectType;
  rejectReason?: string;
}

export interface ModerateRejectRequest {
  rejectType: ModerateRejectType;
  rejectReason?: string;
}

export const approveModerationById = (id: string): PromiseResBody<ModerationResourceRes[]> => {
  return request.post<ModerationResourceRes[]>(`/moderations/${id}/approve`);
};

export const rejectModerationById = (id: string, data: ModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>(`/moderations/${id}/reject`, data);
};

export const approveModerations = (data: BatchModerateRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/approve", data);
};

export const rejectModerations = (data: BatchModerateRejectRequest): PromiseResBody<BatchResult> => {
  return request.post<BatchResult>("/moderations/reject", data);
};

export interface ModerationBaseQuery {
  triggerType?: "UNKNOWN" | "USER_SUBMIT" | "SYSTEM_DETECTED" | "USER_REPORT" | "USER_SUGGESTION" | "USER_FEEDBACK" | "USER_APPEAL";
  priority?: "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW";
  status?: AuditStatus;
  submitterUid?: string;
  submitAtStart?: string;
  submitAtEnd?: string;
}

export interface PostBrief {
  postId?: string;
  title?: string;
  editedTitle?: string;
  author?: UserBrief;
}

export interface SourceContext {
  sourcePostBrief?: PostBrief;
}

export interface UserAdminBrief {
  uid: string;
  displayName: string;
  avatar?: CloudResPresignedUrlResp | null;
  level: number;
  userType: "COMMON" | "ADMIN" | "BOT" | "MODERATOR" | "VIP";
  registrationDate?: string;
  postCount?: string;
  riskLevel?: "LOW" | "MEDIUM" | "HIGH";
}

export interface ImageAuditPayload {
  uuid?: string;
  resourceKey?: string;
  mimeType?: string;
  fileMeta?: FileMeta;
  sourceType?: "SYSTEM" | "CONTENT_ATTACHED" | "USER_UPLOADED";
  uploaderId?: string;
  expiredAt?: ISOString;
  createdAt?: ISOString;
  presignedUrl?: CloudResPresignedUrlResp;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
}

export interface AuditResponseImageAuditPayload {
  taskId?: string;
  targetType?: string;
  triggerType?: string;
  triggerSource?: string;
  priority?: string;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
  status?: AuditStatus;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  submitAt?: ISOString;
  submitter?: UserAdminBrief;
  auditor?: UserBrief;
  payload?: ImageAuditPayload;
  linkedResources?: ModerationResourceRes[];
  sourceContext?: SourceContext;
}

export interface AuditResponsePostAuditPayload {
  taskId?: string;
  targetType?: string;
  triggerType?: string;
  triggerSource?: string;
  priority?: string;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
  status?: AuditStatus;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  submitAt?: ISOString;
  submitter?: UserAdminBrief;
  auditor?: UserBrief;
  payload?: PostAuditPayload;
  linkedResources?: ModerationResourceRes[];
  sourceContext?: SourceContext;
}

export interface ReplyPayload {
  content?: string;
  postId?: string;
  commentId?: string;
  replierUid?: string;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
}

export interface AuditResponseReplyPayload {
  taskId?: string;
  targetType?: string;
  triggerType?: string;
  triggerSource?: string;
  priority?: string;
  format?: "DEFAULT" | "RICH" | "IMAGE" | "TXT";
  status?: AuditStatus;
  rejectType?: ModerateRejectType;
  rejectReason?: string;
  submitAt?: ISOString;
  submitter?: UserAdminBrief;
  auditor?: UserBrief;
  payload?: ReplyPayload;
  linkedResources?: ModerationResourceRes[];
  sourceContext?: SourceContext;
}

export interface UserAuditStats {
  totalPassed: number;
  totalRejected: number;
  passRate: number;
}

export interface ModerationStatsResp {
  pending?: number;
  todayApproved?: number;
  todayRejected?: number;
}

export const listPostModerations = (query: ModerationBaseQuery, page?: number, size?: number): PromiseResBody<Page<AuditResponsePostAuditPayload>> => {
  return request.get<Page<AuditResponsePostAuditPayload>>("/moderations/list/posts", { params: { ...query, page, size } });
};

export const listImageModerations = (query: ModerationBaseQuery, page?: number, size?: number): PromiseResBody<Page<AuditResponseImageAuditPayload>> => {
  return request.get<Page<AuditResponseImageAuditPayload>>("/moderations/list/images", { params: { ...query, page, size } });
};

export const listTextModerations = (query: ModerationBaseQuery, page?: number, size?: number): PromiseResBody<Page<AuditResponseReplyPayload>> => {
  return request.get<Page<AuditResponseReplyPayload>>("/moderations/list/texts", { params: { ...query, page, size } });
};

export const getPostModerationDetail = (id: string | number): PromiseResBody<AuditResponsePostAuditPayload> => {
  return request.get<AuditResponsePostAuditPayload>(`/moderations/posts/${id}`);
};

export const getImageModerationDetail = (id: string | number): PromiseResBody<AuditResponseImageAuditPayload> => {
  return request.get<AuditResponseImageAuditPayload>(`/moderations/images/${id}`);
};

export const getTextModerationDetail = (id: string | number): PromiseResBody<AuditResponseReplyPayload> => {
  return request.get<AuditResponseReplyPayload>(`/moderations/texts/${id}`);
};

export const getModerationStats = (targetType?: TargetType): PromiseResBody<ModerationStatsResp> => {
  return request.get<ModerationStatsResp>("/moderations/stats", { params: { targetType } });
};

export const getUserAuditStats = (auditorUid?: string): PromiseResBody<UserAuditStats> => {
  return request.get<UserAuditStats>("/moderations/user-audit-stats", { params: { auditorUid } });
};
