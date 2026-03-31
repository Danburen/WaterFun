import { PromiseApiRes } from "@waterfun/web-core/src/types/api/response";
import request from "../utils/axiosRequest"
import { Page } from "~/types/api";

export interface UserAdminDto {
  uid: number;
  username: string;
  accountStatus: string;
  statusChangedAt: null;
  statusChangeReason: null;
  updatedAt: string;
  createdAt: string;
  nickname: null;
  avatarUrl: null;
  lastActiveAt: null;
};

export interface UserDetailDto {
  info: Info
  profile: Profile
  counter: Counter
  maskedData: MaskedData
  roles: any[]
  permissions: any[]
}

export interface Info {
  uid: number
  username: string
  accountStatus: string
  statusChangedAt: string
  updatedAt: string
  createdAt: string
  nickname: string
  avatarUrl: string
  lastActiveAt: string
}

export interface Profile {
  bio: string
  gender: string
  birthDate: string
  residence: string
  updateAt: string
}

export interface Counter {
  level: number
  exp: number
  followerCnt: number
  followingCnt: number
  likeCnt: number
  postCnt: number
  updatedAt: string
  visible: boolean
}

export interface MaskedData {
  phoneMasked: string
  emailMasked: string
  phoneVerified: boolean
  emailVerified: boolean
}

export interface updateUserInfoDto{
  nickname: string
  username: string,
  accountStatus: 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED',
  avatarUrl: string
}

export interface updateUserDatumDto{
  email: string
  phone: string
}

export interface updateUserProfileDto{
  bio: string
  gender: string
  birthDate: string
  residence: string
}


export const getUserList = (): PromiseApiRes<Page<UserAdminDto>> => {
    return request.get('/users/list');
}

export const getUserDetail = (uid: number): PromiseApiRes<UserDetailDto> => {
    return request.get(`/users/${uid}`);
}

export const updateUserInfo = (uid: number, data: Partial<Info>): PromiseApiRes<UserDetailDto> => {
    return request.put(`/users/${uid}/info`, data);
}

export const updateUserDatum = (uid: number, data: Partial<updateUserDatumDto>): PromiseApiRes<UserDetailDto> => {
    return request.put(`/users/${uid}/datum`, data);
}

export const updateUserProfile = (uid: number, data: Partial<updateUserProfileDto>): PromiseApiRes<UserDetailDto> => {
    return request.put(`/users/${uid}/profile`, data);
}