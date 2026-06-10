import request from "../utils/axiosRequest"
import type { PromiseResBody, CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response"
import type { UserBrief } from "~/api/postApi"

export interface UserPublicProfileResp {
  uid: number
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
  uid: number
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

export const fetchUserPublicProfile = (uid: number): PromiseResBody<UserPublicProfileResp> => {
  return request.get(`/user/${uid}/profile`)
}

export const fetchUserPublicCard = (uid: number): PromiseResBody<UserPublicCardResp> => {
  return request.get(`/user/${uid}/card`)
}

export const fetchUserPublicAvatar = (uid: number): PromiseResBody<CloudResourceUrlResp> => {
  return request.get(`/user/${uid}/avatar`)
}

export const toggleFollowUser = (uid: number): PromiseResBody<void> => {
  return request.post(`/user/${uid}/follow`)
}

export const fetchFollowings = (uid: number, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
  return request.get(`/user/${uid}/followings`, { params: { page, size } })
}

export const fetchFollowers = (uid: number, page: number = 1, size: number = 20): PromiseResBody<PageUserBrief> => {
  return request.get(`/user/${uid}/followers`, { params: { page, size } })
}
