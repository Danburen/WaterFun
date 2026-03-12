export interface TagNavItemType {
    name: string;
    locale: string;
    to: string;
    closeable: boolean;
    params?: Record<string, string>;
}

export interface BreadNavItemType {
    name: string;
    locale: string;
    to: string | null;
}