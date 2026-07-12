import type {AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig} from "axios";

export type ResBody<T = any> = {
    code: string
    message: string
    data: T
}

export type PromiseResBody<T> = Promise<ResBody<T>>;
// Only have data
export type DataApiResponse<T> = Required<Pick<ResBody<T>, 'data'>> & ResBody<T>;
// only have message
export type MessageApiResponse = Required<Pick<ResBody, 'message'>> & ResBody;

export interface FileResDataType {
    fileName: string;
    fileSize: number;
    lastModified: string; // ISO 8601 格式日期字符串
    content: string;
    fileType: string;
    contentType: string;
}

export type PromiseRawRes<T> = Promise<AxiosResponse<ResBody<T>>>;
interface ApiRes extends AxiosInstance {
    get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ResBody<T>>;
    post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ResBody<T>>;
    put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ResBody<T>>;
    delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ResBody<T>>;

    get(url: string, config: AxiosRequestConfig & { responseType: 'arraybuffer' | 'blob' | 'stream' }): Promise<ArrayBuffer>;
    get(url: string, config: AxiosRequestConfig & { responseType: 'text' }): Promise<string>;
    get<T = unknown, R = ResBody<T>>(url: string, config?: AxiosRequestConfig): Promise<R>;
    post<T = unknown, R = ResBody<T>>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>;
    put<T = unknown, R = ResBody<T>>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>;
    delete<T = unknown, R = ResBody<T>>(url: string, config?: AxiosRequestConfig): Promise<R>;
}

export interface CloudResourceUrlResp {
    url: string;
    expireAt: string;
}

export type ISOString = string | null;

export interface OptionResItem<TId extends number | string = number> {
    id: TId;
    code: string;
    name: string;
    disabled: boolean | null;
}
