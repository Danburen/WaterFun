import request from "~/utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"
import type { Page } from "~/types/api"

export interface DashboardOverview {
  onlineUserCount: number
  totalUsers: number
  totalPosts: number
  todayNewUsers: number
  todayNewPosts: number
  todayPv: number
  pendingModerations: number
}

export interface TrendPoint {
  date: string
  dailyPv: number
  newUsers: number
  newPosts: number
}

export interface UserBrief {
  uid: number
  displayName: string
  avatar?: { url: string; expireAt?: string }
  level: number
  userType: 'COMMON' | 'ADMIN' | 'BOT' | 'MODERATOR' | 'VIP'
}

export interface DashboardRecentActivity {
  userBrief: UserBrief
  lastActiveAt: number
  online: boolean
  actionType: 'UNKNOWN' | 'CREATE' | 'DELETED' | 'UPDATED' | 'INTERACTIVE' | 'REPORT'
  businessType: 'NONE' | 'POST' | 'COMMENT' | 'USER'
  actionTime: string
  targetId: number
  description: string
}

export interface OnlineUserVO {
  uid: number
  userBrief: UserBrief
  lastActive: string
  sessionId: string
  ip: string
}

export interface SiteStatistic {
  statDate: string
  loginCount: number
  dailyPv: number
  newUsers: number
  newPosts: number
  peakOnline: number
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
