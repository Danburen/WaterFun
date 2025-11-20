export interface TableColumns {
    prop: string;
    label: string;
    width?: number;
}

export interface TableData {
    [key: string]: any;
    id?: number | string;
}

export interface TableDatasets {
    data: TableData[]
    columns: TableColumns[]
}