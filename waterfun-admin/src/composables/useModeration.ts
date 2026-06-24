import type { UserBriefWithStats, TicketStatus } from "~/api/tickets";

/** Extract avatar URL from a UserBriefWithStats or string/null */
export function avatarUrl(a: UserBriefWithStats | string | null | undefined): string | null | undefined {
  if (!a) return null;
  if (typeof a === "string") return a;
  return a.url ?? null;
}

/** Open an image URL in a new tab */
export function previewImage(url: string) {
  window.open(url, "_blank");
}

/** Unified status label map — keyed by ticket type domain */
const STATUS_LABEL_MAP: Record<string, Record<string, string>> = {
  ACCOUNT_APPEAL: { PENDING: "待复核", RESOLVED: "已撤销", REJECTED: "已维持" },
  CONTENT_REPORT: { PENDING: "待处理", RESOLVED: "已采纳", REJECTED: "已驳回" },
  FEATURE_FEEDBACK: { PENDING: "未处理", RESOLVED: "已解决", REJECTED: "已拒绝" },
  SUGGESTION: { PENDING: "未处理", RESOLVED: "已解决", REJECTED: "已拒绝" },
};
const STATUS_CHIP_MAP: Record<string, Record<string, string>> = {
  ACCOUNT_APPEAL: { PENDING: "status-pending", RESOLVED: "status-resolved", REJECTED: "status-rejected" },
  CONTENT_REPORT: { PENDING: "status-pending", RESOLVED: "status-resolved", REJECTED: "status-rejected" },
  FEATURE_FEEDBACK: { PENDING: "status-new", RESOLVED: "status-resolved", REJECTED: "status-rejected" },
  SUGGESTION: { PENDING: "status-new", RESOLVED: "status-resolved", REJECTED: "status-rejected" },
};

export function statusLabel(status?: string, ticketType = "ACCOUNT_APPEAL"): string {
  const map = STATUS_LABEL_MAP[ticketType] ?? STATUS_LABEL_MAP.ACCOUNT_APPEAL;
  return map[status ?? ""] ?? status ?? "未知";
}
export function statusChipClass(status?: string, ticketType = "ACCOUNT_APPEAL"): string {
  const map = STATUS_CHIP_MAP[ticketType] ?? STATUS_CHIP_MAP.ACCOUNT_APPEAL;
  return map[status ?? ""] ?? "status-pending";
}

const SNAPSHOT_TYPE_LABEL: Record<string, string> = { POST: "帖子", COMMENT: "评论", IMAGE: "图片" };
const SNAPSHOT_TYPE_CLASS: Record<string, string> = {
  POST: "snapshot-post",
  COMMENT: "snapshot-comment",
  IMAGE: "snapshot-image",
};

export function snapshotTypeLabel(t?: string): string {
  return SNAPSHOT_TYPE_LABEL[t ?? ""] ?? t ?? "内容";
}
export function snapshotTypeClass(t?: string): string {
  return SNAPSHOT_TYPE_CLASS[t ?? ""] ?? "snapshot-comment";
}

/** Reason-type labels shared by reports & appeals */
export function penaltyLabel(t?: string): string {
  const map: Record<string, string> = {
    BAN_LOGIN: "封号处理",
    BAN_POST: "禁言",
    BAN_COMMENT: "禁言",
    BAN_UPLOAD: "禁止上传",
    BAN_CHAT: "禁止聊天",
    BAN_CREATE: "禁止创建",
    DELETE: "仅删除内容",
    WARN: "警告",
    MUTE: "禁言",
    UNSPECIFIED: "未指定",
    OTHER: "其他处罚",
    VIOLATION_OF_GUIDELINES: "违规内容",
    INAPPROPRIATE_CONTENT: "不当内容",
    ADVERTISEMENT: "广告",
    VIOLENCE: "暴力",
    SENSITIVE: "敏感内容",
    CHEATING: "作弊",
    IMPERSONATION: "冒充",
    PRIVACY: "隐私",
    TROLLING: "引战",
  };
  return map[t ?? ""] ?? t ?? "违规处罚";
}
