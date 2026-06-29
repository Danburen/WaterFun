import request from "../utils/axiosRequest"
import type { PromiseResBody, CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response";

export interface CloudResPresignedUrlResp {
  url: string
  expireAt: string | null
}

export interface UserBrief {
  uid: string
  displayName: string
  avatar: CloudResourceUrlResp | null
  level: number
  userType: 'COMMON' | 'ADMIN' | 'BOT' | 'MODERATOR' | 'VIP'
}

export interface PostCardResp {
  id: string
  title: string
  subtitle: string
  summary: string
  userBrief: UserBrief | null
  coverImage: CloudResourceUrlResp | null
  category: OptionVOLong | null
  tags: OptionVOLong[]
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  slug: string
  publishedAt: string | null
  type?: 'COMMON' | 'NOTICE'
  isPinned?: boolean
}

export interface PostAuthorCardResp extends PostCardResp {
  visibility: 'PUBLIC' | 'PRIVATE' | 'FANS_ONLY'
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED'
  editStatus: 'NONE' | 'PENDING' | 'REJECTED'
  updatedAt: string | null
  editedTitle?: string
  editedContent?: string
  editedSummary?: string
}

export interface PostDetailResp {
  id: string
  title: string
  subtitle: string
  content: string
  summary: string
  coverImage: CloudResourceUrlResp | null
  category: OptionVOLong | null
  tags: OptionVOLong[]
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  slug: string
  publishedAt: string | null
  updatedAt: string | null
  type: 'COMMON' | 'NOTICE'
  isPinned: boolean
  userBrief: UserBrief | null
  isLiked: boolean
  isCollected: boolean
  visibility: 'PUBLIC' | 'PRIVATE' | 'FANS_ONLY'
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED'
  editStatus: 'NONE' | 'PENDING' | 'REJECTED'
}

export interface PostDraftResp {
  editedTitle: string
  editedContent: string
  editedSummary: string
  coverageImgPresignedUrl: CloudResPresignedUrlResp | null
  editedCoverImg: string | null
  editedCategoryId: OptionVOLong | null
  editedTagIds: OptionVOLong[]
  editedNewTagIds: string[]
  editedStatus: 'NONE' | 'PENDING'
}

export interface OptionVOLong {
  id: string
  code: string
  name: string
  disabled: boolean
  usageCount?: number
}

export interface PostSaveReq {
  title: string
  subtitle?: string
  content: string
  summary?: string
  coverageImgId?: string
  newTags?: string[]
  tagIds?: string[]
  categoryId?: string | null
}

export interface CategoryResponse {
  id: string
  name: string
  slug: string
  description: string
  parent: OptionVOLong | null
  sortOrder: number
  isActive: boolean
  usageCount: number
  createdAt: string | null
}

export interface TagResponse {
  id: string
  name: string
  slug: string
  description: string
  usageCount: number
  createdAt: string | null
}

export interface SortObject {
  empty?: boolean
  sorted?: boolean
  unsorted?: boolean
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  sort?: SortObject
  first: boolean
  last: boolean
  numberOfElements?: number
  empty: boolean
}

export interface PostListParams {
  categoryId?: string
  tagIds?: string[]
  keyword?: string
  authorId?: string
  page?: number
  size?: number
}

export interface PublicPostListReq {
  categoryId?: string
  tagIds?: string[]
  keyword?: string
  authorId?: string
  page?: number
  size?: number
}

export interface MyPostsStatsResp {
  totalCount: number
  publishedCount: number
  draftCount: number
  pendingCount: number
  rejectedCount: number
  totalLikeCount: number
  followerCount: number
}

export interface ContentPreviewReq {
  content: string
}

export interface CreateReportReq {
  type: string
  reason?: string
  reasonValid?: boolean
}

export interface MyPostListParams {
  title?: string
  status?: string
  visibility?: string
  categoryId?: string
  tagIds?: string[]
  page?: number
  size?: number
  sort?: string
}

export const fetchPostList = (params: PostListParams): PromiseResBody<PageResult<PostCardResp>> => {
  return request.get('/posts/list', { params })
}

export const fetchPostDetail = (id: string): PromiseResBody<PostDetailResp> => {
  return request.get(`/posts/${id}`)
}

export const deletePost = (id: string): PromiseResBody<void> => {
  return request.delete(`/posts/${id}`)
}

export const fetchMyPostDetail = (id: string): PromiseResBody<PostDetailResp> => {
  // Merged into GET /api/posts/{id} — returns full detail including visibility/status/editStatus
  return request.get(`/posts/${id}`)
}

export const fetchMyPostStats = (): PromiseResBody<MyPostsStatsResp> => {
  return request.get('/posts/me/stats')
}

export const fetchMyPostList = (params: MyPostListParams): PromiseResBody<PageResult<PostAuthorCardResp>> => {
  return request.get('/posts/me/list', { params })
}

export const createDraft = (): PromiseResBody<string> => {
  return request.post('/posts/draft')
}

export const fetchEditDraft = (id: string): PromiseResBody<PostDraftResp> => {
  return request.get(`/posts/${id}/edit`)
}

export const publishPost = (id: string, data: PostSaveReq): PromiseResBody<void> => {
  return request.post(`/posts/${id}/publish`, data)
}

export const previewContent = (id: string, content: string): PromiseResBody<string> => {
  return request.post(`/posts/${id}/content/preview`, { content })
}

export const tempSavePost = (id: string, data: PostSaveReq): PromiseResBody<void> => {
  return request.post(`/posts/${id}/temp-save`, data)
}

export const fetchCategories = (): PromiseResBody<OptionVOLong[]> => {
  return request.get('/post/category/options')
}

export const fetchTags = (): PromiseResBody<TagResponse[]> => {
  return request.get('/post/tags/me')
}

export const likePost = (id: string): PromiseResBody<void> => {
  return request.post(`/posts/${id}/like`)
}

export const collectPost = (id: string): PromiseResBody<void> => {
  return request.post(`/posts/${id}/collection`)
}

export const categoryOptions = (): PromiseResBody<OptionVOLong[]> => {
  return request.get('/post/category/options')
}

export const searchTags = (keyword: string, limit: number = 10): PromiseResBody<TagResponse[]> => {
  return request.get('/post/tags/search', { params: { keyword, limit } })
}

export const searchTagOptions = (keyword: string, limit: number = 10): PromiseResBody<OptionVOLong[]> => {
  return request.get('/post/tags/search/options', { params: { keyword, limit } })
}

export const getTag = (id: string): PromiseResBody<TagResponse> => {
  return request.get(`/post/tags/${id}`)
}

export const deleteTag = (id: string): PromiseResBody<void> => {
  return request.delete(`/post/tags/${id}`)
}

export const getHotTags = (params: { page?: number; size?: number; sort?: string[] }): PromiseResBody<PageResult<TagResponse>> => {
  return request.get('/post/tags/hot', { params })
}

export const publishNewPost = (data: PostSaveReq): PromiseResBody<number> => {
  return request.post('/posts/publish', data)
}

export const tempSaveNewPost = (data: PostSaveReq): PromiseResBody<number> => {
  return request.post('/posts/temp-save', data)
}

export const previewContentAlone = (content: string): PromiseResBody<string> => {
  return request.post('/posts/content/preview', { content })
}

export const batchPublishPosts = (ids: string[]): PromiseResBody<void> => {
  return request.post('/posts/me/batch-publish', ids)
}

export const batchDeletePosts = (ids: string[]): PromiseResBody<void> => {
  return request.post('/posts/me/batch-delete', ids)
}

export const getPostLikedUsers = (id: string): PromiseResBody<UserBrief[]> => {
  return request.get(`/posts/${id}/liked-users`)
}

export const reportPost = (id: string, data: CreateReportReq): PromiseResBody<{ taskId: string }> => {
  return request.post(`/posts/${id}/report`, data)
}

export const cancelReportPost = (id: string): PromiseResBody<void> => {
  return request.post(`/posts/${id}/report/cancel`)
}

export const fetchHotPosts = (params: {
  page?: number
  size?: number
} = {}): PromiseResBody<PageResult<PostCardResp>> => {
  return request.get('/posts/hot', { params })
}

export const fetchAnnouncements = (params: {
  page?: number
  size?: number
} = {}): PromiseResBody<PageResult<PostCardResp>> => {
  return request.get('/announcements', { params })
}
