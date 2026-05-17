export interface CacheItem {
    presignedUrl: string;
    expiresAt: number;
    lastAccess: number;
}

export default CacheItem
