import request from "../utils/axiosRequest"
import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response"
import type { UserBrief } from "~/api/postApi"
import type { ReportType } from "~/api/ticketApi"

export interface CommentResponse {
  id: string
  postId: string
  parentId: string
  rootId: string
  author: UserBrief
  content: string
  likeCount: number
  replyCount: number
  createdAt: string
  replyToDisplayName: string
  isPostAuthor?: boolean
  isLiked?: boolean
}

export interface CursorPageComment<T, C = number> {
  list: T[]
  nextCursor: C | null
  hasNext: boolean
}

export interface CreateCommentReq {
  postId: string
  parentId?: string
  content: string
}

export const postComment = (data: CreateCommentReq): PromiseResBody<void> => {
  return request.post('/comments', data)
}

export const listComments = (params: {
  postId: string
  cursor?: string
  limit?: number
  includeRootId?: string
}): PromiseResBody<CursorPageComment<CommentResponse, string>> => {
  return request.get('/comments/list', { params })
}

export const listReplies = (rootId: string, params: {
  cursor?: string
  limit?: number
  includeRootId?: string
}): PromiseResBody<CursorPageComment<CommentResponse, string>> => {
  return request.get(`/comments/${rootId}/replies`, { params })
}

export const getComment = (commentId: string): PromiseResBody<CommentResponse> => {
  return request.get(`/comments/${commentId}`)
}

export const likeComment = (id: string): PromiseResBody<void> => {
  return request.post(`/comments/${id}/like`)
}

export const deleteComment = (id: string): PromiseResBody<void> => {
  return request.delete(`/comments/${id}`)
}

export const reportComment = (id: string, data: { type: ReportType; reason?: string }): PromiseResBody<{ taskId: string }> => {
  return request.post(`/comments/${id}/report`, data)
}

export const cancelReportComment = (id: string): PromiseResBody<void> => {
  return request.post(`/comments/${id}/report/cancel`)
}
