import type { ISOString, PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import request from "~/utils/axiosRequest";

export type NotificationType = "GENERAL" | "LIKE" | "REPLY" | "MENTION" | "NEW_FOLLOWER" | "COLLECT" | "PROMOTION" | "SYSTEM";
export type NotificationGroup = "SYSTEM" | "INTERACTION" | "MISC" | "REPLY" | "MENTION";

export interface InboxNotificationRes {
  id: number;
  title: string;
  noticeType: number;
  content: {
    text: string;
    refId?: number;
  };
  createdAt: ISOString;
  isRead: boolean;
}

export interface CursorPageInboxNotificationRes {
  list: InboxNotificationRes[];
  nextCursor: number | null;
  hasNext: boolean;
}

export interface BatchMarkReadReq {
  ids: number[];
}

export const getUnreadCount = (): PromiseResBody<number> => {
  return request.get<number>("/notifications/unreadCount");
};

export const listNotifications = (params: {
  cursor?: number;
  limit?: number;
  unreadOnly?: boolean;
  type?: NotificationType;
  group?: NotificationGroup;
} = {}): PromiseResBody<CursorPageInboxNotificationRes> => {
  return request.get<CursorPageInboxNotificationRes>("/notifications/list", { params });
};

export const markNotificationRead = (id: number): PromiseResBody<null> => {
  return request.post<null>(`/notifications/read/${id}`);
};

export const markAllNotificationsRead = (): PromiseResBody<null> => {
  return request.post<null>("/notifications/markAllRead");
};

export const batchMarkNotificationsRead = (data: BatchMarkReadReq): PromiseResBody<null> => {
  return request.post<null>("/notifications/batchMarkRead", data);
};

export const deleteNotification = (id: number): PromiseResBody<null> => {
  return request.delete<null>(`/notifications/${id}`);
};
