import request from "~/utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"
import type { Page } from "~/types/api"
import type { UserBrief } from "~/api/dashboard"

export interface OnlineUserVO {
  uid: string
  userBrief: UserBrief
  lastActive: string
  sessionId: string
  ip: string
}

export interface OnlineCountVO {
  onlineCount: string
  adminOnlineCount: string
  pealOnlineCount: string
}

export interface ListOnlineUsersParams {
  page?: number
  size?: number
  keyword?: string
  userType?: 'COMMON' | 'ADMIN' | 'BOT' | 'MODERATOR' | 'VIP'
  levelMin?: number
  levelMax?: number
}

export const listOnlineUsers = (params: ListOnlineUsersParams = {}): PromiseResBody<Page<OnlineUserVO>> => {
  return request.get<Page<OnlineUserVO>>('/online-users', { params })
}

export const getOnlineCount = (): PromiseResBody<OnlineCountVO> => {
  return request.get<OnlineCountVO>('/online-users/count')
}

export const forceOffline = (uid: number | string): PromiseResBody<null> => {
  return request.post<null>(`/online-users/${uid}/force-offline`)
}
