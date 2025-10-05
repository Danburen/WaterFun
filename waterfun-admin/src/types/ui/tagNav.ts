export interface TagNavItemType {
    name: string;
    locale: string;
    to: string;
    closeable: boolean;
}

export interface BreadNavItemType {
    name: string;
    locale: string;
    to: string | null;
}