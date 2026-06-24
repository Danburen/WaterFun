import request from "~/utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"
import type { Page } from "~/types/api"

export interface DashboardOverview {
  onlineUserCount: string
  totalUsers: string
  totalPosts: string
  todayNewUsers: string
  todayNewPosts: string
  todayPv: string
  pendingModerations: string
  peakOnline?: string
}

export interface TrendPoint {
  date: string
  dailyPv: string
  newUsers: string
  newPosts: string
}

export interface UserBrief {
  uid: number
  displayName: string
  avatar?: { url?: string; expireAt?: string } | null
  level: number
  userType: 'COMMON' | 'ADMIN' | 'BOT' | 'MODERATOR' | 'VIP'
}

export interface DashboardRecentActivity {
  userBrief: UserBrief
  lastActiveAt: string
  online: boolean
  actionType: 'UNKNOWN' | 'CREATE' | 'DELETED' | 'UPDATED' | 'INTERACTIVE' | 'REPORT'
  businessType: 'NONE' | 'POST' | 'COMMENT' | 'USER' | 'TICKET_REPLY'
  actionTime: string
  targetId: string
  description: string
}

export interface OnlineUserVO {
  uid: string
  userBrief: UserBrief
  lastActive: string
  sessionId: string
  ip: string
}

export interface SiteStatistic {
  statDate: string
  loginCount: string
  dailyPv: string
  newUsers: string
  newPosts: string
  peakOnline: string
  updatedAt: string
}

export const getDashboardOverview = (): PromiseResBody<DashboardOverview> => {
  return request.get<DashboardOverview>('/dashboard/overview')
}

export const getTrend = (days: number = 7): PromiseResBody<TrendPoint[]> => {
  return request.get<TrendPoint[]>('/dashboard/trend', { params: { days } })
}

export const getRecentActivities = (limit: number = 10): PromiseResBody<DashboardRecentActivity[]> => {
  return request.get<DashboardRecentActivity[]>('/dashboard/recent-activities', { params: { limit } })
}

export const getDashboardOnlineUsers = (page: number = 0, size: number = 10): PromiseResBody<Page<OnlineUserVO>> => {
  return request.get<Page<OnlineUserVO>>('/dashboard/online-users', { params: { page, size } })
}

export const getLatestStatistic = (): PromiseResBody<SiteStatistic> => {
  return request.get<SiteStatistic>('/dashboard/statistics/latest')
}
