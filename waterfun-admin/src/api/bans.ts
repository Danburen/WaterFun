import type { PromiseResBody } from "@waterfun/web-core/src/types/api/response";
import type { Page } from "~/types/api";
import request from "~/utils/axiosRequest";

export interface BanUserResponse {
  userUid: string;
  displayName?: string;
  nickname?: string;
  permissionName: string;
  permissionCode: string;
  expiresAt?: string;
  createdAt: string;
  operatorName?: string;
}

export type PenaltyType =
  | "UNSPECIFIED"
  | "BAN_LOGIN"
  | "BAN_POST"
  | "BAN_COMMENT"
  | "BAN_UPLOAD"
  | "BAN_CHAT"
  | "BAN_CREATE"
  | "OTHER";

export interface BanUserRequest {
  userUid: string;
  penaltyType: PenaltyType;
  banReasonType?: string;
  penaltyDurationHours?: string;
  reasonText?: string;
}

export interface LiftPenaltyRequest {
  userUid: string;
  penaltyType?: PenaltyType;
}

export interface ListBanParams {
  userUid?: string;
  username?: string;
  nickname?: string;
  page?: number;
  size?: number;
}

export const listBans = (params: ListBanParams = {}): PromiseResBody<Page<BanUserResponse>> => {
  return request.get<Page<BanUserResponse>>("/bans", { params });
};

export const banUser = (data: BanUserRequest): PromiseResBody<null> => {
  return request.post<null>("/bans", data);
};

export const liftPenalty = (userUid: string | number, data: LiftPenaltyRequest): PromiseResBody<null> => {
  return request.post<null>(`/bans/${userUid}/lift`, data);
};

export const liftAllPenalties = (userUid: string | number): PromiseResBody<null> => {
  return request.post<null>(`/bans/${userUid}/lift-all`);
};

export interface BanStatusResponse {
  userUid: string;
  banned: boolean;
  restrictions: ActiveRestriction[];
}

export interface ActiveRestriction {
  permissionCode: string;
  permissionName: string;
  banReasonType: string | null;
  expiresAt: string | null;
  permanent: boolean;
  createdAt: string;
}

export const getBanStatus = (userUid: string | number): PromiseResBody<BanStatusResponse> => {
  return request.get<BanStatusResponse>("/bans/status", { params: { userUid: String(userUid) } });
};
