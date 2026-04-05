export interface Page<T> {
    content: T[];
    page: Pagination;
}

export interface Pagination {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
}

export interface PageOptions {
    total?: number;
    pageSize?: number;
    currentPage?: number;
}