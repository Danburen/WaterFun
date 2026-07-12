/**
 * A LRU cache for image memory.
 * 
 */
import type { CacheItem } from './types'

const cache = new Map<String, CacheItem>();
const MAX = 150;

/**
 * Get the image memory from the cache.
 * @param path The image path.
 * @returns The image memory and metadata, or undefined if not found or expired.
 */
export function getMemory(path: string): CacheItem | undefined {
    const item = cache.get(path);
    if (!item) return undefined;
    
    if (item.expiresAt && item.expiresAt < Date.now()) {
        cache.delete(path);
        return undefined;
    }
    
    item.lastAccess = Date.now();
    cache.set(path, item);
    
    return item;
}

/**
 * Set the image memory to the cache.
 * @param path The image path.
 * @param presignedUrl Optional presigned URL.
 * @param expiresAt Optional expiration time in milliseconds since epoch.
 */
export function setMemory(path: string, presignedUrl: string, expiresAt: number): void {
    if (cache.has(path)) {
        cache.delete(path);
    }
    
    if (cache.size >= MAX) {
        const firstEntry = cache.entries().next().value;
        if (firstEntry) {
            const [firstKey] = firstEntry;
            cache.delete(firstKey);
        }
    }
    
    cache.set(path, {
        presignedUrl,
        expiresAt,
        lastAccess: Date.now()
    });
}

/**
 * Revoke the image memory from the cache.
 * @param path The image path.
 */
export function revokeMemory(path: string): void {
    cache.delete(path);
}
