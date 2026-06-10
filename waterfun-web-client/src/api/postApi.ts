import request from "../utils/axiosRequest"
import type { PromiseResBody, CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response";

export interface CloudResPresignedUrlResp {
  url: string
  expireAt: string | null
}

export interface OptionVOInteger {
  id: number
  code: string
  name: string
  disabled: boolean
}

export interface UserBrief {
  uid: number
  displayName: string
  avatar: CloudResourceUrlResp
  level: number
  userType: 'COMMON' | 'ADMIN' | 'BOT' | 'MODERATOR' | 'VIP'
}

export interface PostCardResp {
  id: number
  title: string
  subtitle: string
  summary: string
  userBrief?: UserBrief
  coverImage: CloudResourceUrlResp | null
  category: OptionVOInteger | null
  tags: OptionVOInteger[]
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
}

export interface PostDetailResp {
  id: number
  title: string
  subtitle: string
  content: string
  summary: string
  coverImage: CloudResourceUrlResp | null
  category: OptionVOInteger | null
  tags: OptionVOInteger[]
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  slug: string
  publishedAt: string | null
  updatedAt: string | null
  type?: 'COMMON' | 'NOTICE'
  isPinned?: boolean
}

export interface PostAuthorDetailResp extends PostDetailResp {
  visibility: 'PUBLIC' | 'PRIVATE' | 'FANS_ONLY'
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED'
}

export interface PostDraftResp {
  editedTitle: string
  editedContent: string
  editedSummary: string
  coverageImgPresignedUrl: CloudResPresignedUrlResp | null
  editedCategoryId: OptionVOLong | null
  editedTagIds: OptionVOLong[]
  editedNewTagIds: string[]
  editedStatus: 'NONE' | 'PENDING'
}

export interface OptionVOLong {
  id: number
  code: string
  name: string
  disabled: boolean
}

export interface PostSaveReq {
  title: string
  subtitle?: string
  content: string
  summary?: string
  coverageImgId?: string
  newTags?: string[]
  tagIds?: number[]
  categoryId: number
}

export interface CategoryResponse {
  id: number
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
  id: number
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
  categoryId?: number
  tagIds?: number[]
  authorId?: number
  page?: number
  size?: number
}

export interface MyPostListParams {
  status?: string
  visibility?: string
  categoryId?: number
  tagIds?: number[]
  page?: number
  size?: number
}

export const fetchPostList = (params: PostListParams): PromiseResBody<PageResult<PostCardResp>> => {
  return request.get('/posts/list', { params })
}

export const fetchPostDetail = (id: number): PromiseResBody<PostDetailResp> => {
  return request.get(`/posts/${id}`)
}

export const deletePost = (id: number): PromiseResBody<void> => {
  return request.delete(`/posts/${id}`)
}

export const fetchMyPostDetail = (id: number): PromiseResBody<PostAuthorDetailResp> => {
  return request.get(`/posts/me/${id}`)
}

export const fetchMyPostList = (params: MyPostListParams): PromiseResBody<PageResult<PostAuthorCardResp>> => {
  return request.get('/posts/me/list', { params })
}

export const createDraft = (): PromiseResBody<number> => {
  return request.post('/posts/draft')
}

export const fetchEditDraft = (id: number): PromiseResBody<PostDraftResp> => {
  return request.get(`/posts/${id}/edit`)
}

export const publishPost = (id: number, data: PostSaveReq): PromiseResBody<void> => {
  return request.post(`/posts/${id}/publish`, data)
}

export const previewContent = (id: number, content: string): PromiseResBody<string> => {
  return request.get(`/posts/${id}/content/preview`, { data: { content } })
}

export const tempSavePost = (id: number, data: PostSaveReq): PromiseResBody<void> => {
  return request.post(`/posts/${id}/temp-save`, data)
}

export const fetchCategories = (): PromiseResBody<OptionVOLong[]> => {
  return request.get('/post/category/options')
}

export const fetchTags = (): PromiseResBody<TagResponse[]> => {
  return request.get('/post/tags/me')
}

export const likePost = (id: number): PromiseResBody<void> => {
  return request.post(`/posts/${id}/like`)
}

export const collectPost = (id: number): PromiseResBody<void> => {
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

export const getTag = (id: number): PromiseResBody<TagResponse> => {
  return request.get(`/post/tags/${id}`)
}

export const deleteTag = (id: number): PromiseResBody<void> => {
  return request.delete(`/post/tags/${id}`)
}

export const getHotTags = (params: { page?: number; size?: number; sort?: string[] }): PromiseResBody<PageResult<TagResponse>> => {
  return request.get('/post/tags/hot', { params })
}
