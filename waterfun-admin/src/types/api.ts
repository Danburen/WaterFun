export interface Page<T> {
    content: T[];
    totalElements?: string;
    totalPages?: number;
    number?: number;
    size?: number;
    first?: boolean;
    last?: boolean;
    empty?: boolean;
    page?: {
        size: number;
        number: number;
        totalElements: string;
        totalPages: number;
    };
}

export interface PageOptions {
    total?: number | string;
    pageSize?: number;
    currentPage?: number;
}

