export interface TableColumns {
    prop: string;
    label: string;
    width?: number;
}

export interface TableData {
    [key: string]: any;
}

export interface TableDatasets {
    data: TableData[]
    columns: TableColumns[]
}