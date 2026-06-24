import request from "../utils/axiosRequest"
import type { PromiseResBody, CloudResourceUrlResp } from "@waterfun/web-core/src/types/api/response"

export interface BannerResponse {
  id: string
  title: string
  subtitle: string
  linkUrl: string
  position: 'HOME' | 'SIDE'
  sortNo: number
  startAt: string | null
  endAt: string | null
  presignedUrl: CloudResourceUrlResp | null
}

export type BannerPosition = 'HOME' | 'SIDE'

export const getAllBanners = (): PromiseResBody<BannerResponse[]> => {
  return request.get('/banners')
}

export const getBannersByPosition = (position: BannerPosition): PromiseResBody<BannerResponse[]> => {
  return request.get('/banners/by-position', { params: { position } })
}
