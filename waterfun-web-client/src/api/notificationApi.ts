import request from "../utils/axiosRequest";
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";

export interface InboxNotificationRes {
  id: string;
  title: string;
  noticeType: number;
  content: NotificationContent;
  createdAt: string;
  isRead: boolean;
}

export interface NotificationContent {
  displayText: string;
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
  ids: (number | bigint)[];
}

export interface BatchResult {
  requested: number;
  success: number;
  ignored: number;
  failed: number;
}

export const listNotifications = (
  params: ListNotificationParams
): PromiseResBody<CursorPageLong<InboxNotificationRes>> => {
  return request.get("/notifications/list", { params });
};

export const getUnreadCount = (): PromiseResBody<number> => {
  return request.get("/notifications/unreadCount");
};

export const markNotificationRead = (id: string): PromiseResBody<void> => {
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

export const deleteNotification = (id: string): PromiseResBody<void> => {
  return request.delete(`/notifications/${id}`);
};
