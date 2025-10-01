export type ApiResponse<T = any> = {
    code: number
    message: string
    data: T
}
// Only have data
export type DataApiResponse<T> = Required<Pick<ApiResponse<T>, 'data'>> & ApiResponse<T>;
// only have message
export type MessageApiResponse = Required<Pick<ApiResponse, 'message'>> & ApiResponse;