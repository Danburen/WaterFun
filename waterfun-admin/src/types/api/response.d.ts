export type ApiResponse<T = any> = {
    code: number
    message: string
    data: T
}
// Only have data
export type DataApiResponse<T> = Required<Pick<ApiResponse<T>, 'data'>> & ApiResponse<T>;
// only have message
export type MessageApiResponse = Required<Pick<ApiResponse, 'message'>> & ApiResponse;

export interface FileResDataType {
    fileName: string;
    fileSize: number;
    lastModified: string; // ISO 8601 格式日期字符串
    content: string;
    fileType: string;
    contentType: string;
}

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