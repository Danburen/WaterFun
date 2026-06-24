import request from "../utils/axiosRequest"
import type { PromiseResBody, CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response"
import type { UserBrief } from "~/api/postApi"

export interface UserPublicProfileResp {
  uid: string
  userBrief: UserBrief
  bio: string
  gender: string
  birthday: string
  residence: string
  createdAt: string | null
  followers: number
  followings: number
  likeCount: number
  postCount: number
}

export interface UserPublicCardResp {
  uid: string
  userBrief: UserBrief
  followers: number
  followings: number
  likeCount: number
  postCount: number
}

export interface PageUserBrief {
  content: UserBrief[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const fetchUserPublicProfile = (uid: string): PromiseResBody<UserPublicProfileResp> => {
  return request.get(`/user/${uid}/profile`)
}

export const fetchUserPublicCard = (uid: string): PromiseResBody<UserPublicCardResp> => {
  return request.get(`/user/${uid}/card`)
}

export const fetchUserPublicAvatar = (uid: string): PromiseResBody<CloudResourceUrlResp> => {
  return request.get(`/user/${uid}/avatar`)
}

export const toggleFollowUser = (uid: string): PromiseResBody<void> => {
  return request.post(`/user/${uid}/follow`)
}

export const fetchFollowings = (uid: string, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
  return request.get(`/user/${uid}/followings`, { params: { page, size } })
}

export const fetchFollowers = (uid: string, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
  return request.get(`/user/${uid}/followers`, { params: { page, size } })
}
