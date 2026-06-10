import request from "../utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"
import type { UserBrief } from "~/api/postApi"

export interface CommentResponse {
  id: number
  postId: number
  parentId: number
  rootId: number
  author: UserBrief
  content: string
  likeCount: number
  replyCount: number
  createdAt: string
  replyToDisplayName: string
}

export interface CursorPageComment<T, C = string> {
  list: T[]
  nextCursor: C | null
  hasNext: boolean
}

export interface CreateCommentReq {
  postId: number
  parentId?: number
  content: string
}

export const postComment = (data: CreateCommentReq): PromiseResBody<void> => {
  return request.post('/comments', data)
}

export const listComments = (params: {
  postId: number
  cursor?: string
  limit?: number
  includeRootId?: number
}): PromiseResBody<CursorPageComment<CommentResponse, string>> => {
  return request.get('/comments/list', { params })
}

export const listReplies = (rootId: number, params: {
  cursor?: number
  limit?: number
  includeRootId?: number
}): PromiseResBody<CursorPageComment<CommentResponse, number>> => {
  return request.get(`/comments/${rootId}/replies`, { params })
}

export const getComment = (commentId: number): PromiseResBody<CommentResponse> => {
  return request.get(`/comments/${commentId}`)
}

export const likeComment = (id: number): PromiseResBody<void> => {
  return request.post(`/comments/${id}/like`)
}

export const deleteComment = (id: number): PromiseResBody<void> => {
  return request.delete(`/comments/${id}`)
}
