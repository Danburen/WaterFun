import request from "../utils/axiosRequest";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { CloudResPresignedUrlResp } from "./postApi";

export interface InboxNotificationRes {
  id: number;
  title: string;
  noticeType: number;
  content: NotificationContent;
  createdAt: string;
  isRead: boolean;
}

export interface NotificationContent {
  userUids?: number[]
  replierUid?: number
  replyContent?: string
  nativeUrl?: string
  text?: string
  followerUid?: number
  postCoverage?: CloudResPresignedUrlResp | null
}

export interface CursorPageLong<T> {
  list: T[];
  nextCursor: number | null;
  hasNext: boolean;
}

export type NotificationType = 'GENERAL' | 'LIKE' | 'REPLY' | 'MENTION' | 'NEW_FOLLOWER' | 'COLLECT' | 'PROMOTION' | 'SYSTEM';

export type NotificationGroup = 'SYSTEM' | 'INTERACTION' | 'MISC' | 'REPLY' | 'MENTION';

export interface ListNotificationParams {
  cursor?: number
  limit?: number
  unreadOnly?: boolean
  type?: NotificationType
  group?: NotificationGroup
}

export interface BatchMarkReadReq {
  ids: number[];
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export interface UnreadCountResp {
  total: number;
  tabs: Record<string, number>;
}

export function getNotificationText(item: InboxNotificationRes): string {
  const c = item.content
  if (!c) return ''
  switch (item.noticeType) {
    case 2:
    case 3:
      return c.replyContent || ''
    case 1:
      return `获得 ${c.userUids?.length ?? 1} 个赞`
    case 5:
      return `被 ${c.userUids?.length ?? 1} 人收藏`
    case 4:
      return '新关注者'
    case 9:
    case 10:
      return c.text || ''
    default:
      return c.text || c.replyContent || ''
  }
}

export const listNotifications = (
  params: ListNotificationParams
): PromiseResBody<CursorPageLong<InboxNotificationRes>> => {
  return request.get("/notifications/list", { params });
};

export const getUnreadCount = (): PromiseResBody<UnreadCountResp> => {
  return request.get("/notifications/unreadCount");
};

export const markNotificationRead = (id: number): PromiseResBody<void> => {
  return request.post(`/notifications/read/${id}`);
};

export const markAllNotificationsRead = (): PromiseResBody<void> => {
  return request.post("/notifications/markAllRead");
};

export const batchMarkNotificationsRead = (
  data: BatchMarkReadReq
): PromiseResBody<BatchResult> => {
  return request.post("/notifications/batchMarkRead", data);
};

export const deleteNotification = (id: number): PromiseResBody<void> => {
  return request.delete(`/notifications/${id}`);
};
