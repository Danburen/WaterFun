export interface Page<T> {
    content: T[];
    totalElements?: number;
    totalPages?: number;
    number?: number;
    size?: number;
    first?: boolean;
    last?: boolean;
    empty?: boolean;
}

export interface PageOptions {
    total?: number | string;
    pageSize?: number;
    currentPage?: number;
}

export interface BatchResult {
    requested: number;
    success: number;
    ignored: number;
    failed: number;
}

