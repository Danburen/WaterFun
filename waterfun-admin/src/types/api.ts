export interface Page<T> {
    content: T[];
    totalElements?: number;
    totalPages?: number;
    number?: number;
    size?: number;
    first?: boolean;
    last?: boolean;
    empty?: boolean;
    page?: {
        size: number;
        number: number;
        totalElements: number;
        totalPages: number;
    };
}

export interface PageOptions {
    total?: number;
    pageSize?: number;
    currentPage?: number;
}

