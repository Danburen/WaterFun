import {DataApiResponse} from "@/types/api/response.ts";

export interface UserInfoResponse {
    username: string;
    nickname: string;
    avatar: string;
    userId: number;
}

export interface UserDataResponse {
    accessToken: string;
    exp: number
}

export type LoginResponseDataType = DataApiResponse<UserDataResponse>;