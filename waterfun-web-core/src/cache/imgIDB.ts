
/**
 * A IDB cache for image memory.
 */

import type { CacheItem } from './types'

// Check if we're in a browser environment
const isClient = typeof window !== 'undefined' && typeof window.localStorage !== 'undefined';

// Prefix for all cache keys to avoid conflicts with other localStorage data
const CACHE_KEY_PREFIX = 'imgCache_';

// Create a simple wrapper around localStorage
const storage = {
    async getItem(key: string): Promise<CacheItem | null> {
        if (!isClient) return null;
        const item = localStorage.getItem(CACHE_KEY_PREFIX + key);
        if (!item) return null;
        
        try {
            return JSON.parse(item);
        } catch (error) {
            console.error('Failed to parse cache item:', error);
            return null;
        }
    },
    
    async setItem(key: string, value: CacheItem): Promise<void> {
        if (!isClient) return;
        localStorage.setItem(CACHE_KEY_PREFIX + key, JSON.stringify(value));
    },
    
    async removeItem(key: string): Promise<void> {
        if (!isClient) return;
        localStorage.removeItem(CACHE_KEY_PREFIX + key);
    },
    
    async keys(): Promise<string[]> {
        if (!isClient) return [];
        return Object.keys(localStorage).filter(key => key.startsWith(CACHE_KEY_PREFIX))
            .map(key => key.substring(CACHE_KEY_PREFIX.length));
    }
};

/**
 * Get the image memory from the IDB.
 * @param path The image path.
 * @returns The image memory and metadata, or undefined if not found or expired.
 */
export const getIDB = async (path: string): Promise<CacheItem | undefined> => {
    const item = await storage.getItem(path);
    if (!item) return undefined;
    
    if (item.expiresAt && item.expiresAt < Date.now()) {
        await storage.removeItem(path);
        return undefined;
    }
    
    item.lastAccess = Date.now();
    await storage.setItem(path, item);
    
    return item;
};

/**
 * Set the image memory to the IDB.
 * @param path The image path.
 * @param presignedUrl Optional presigned URL.
 * @param expiresAt Optional expiration time in milliseconds since epoch.
 */
export const setIDB = async (path: string, presignedUrl: string, expiresAt: number): Promise<void> => {
    const item: CacheItem = {
        presignedUrl,
        expiresAt,
        lastAccess: Date.now()
    };
    await storage.setItem(path, item);
};

/**
 * Clear the expired image memory from the IDB.
 * @param days The expired days. Default is 7 days.
 */
export async function clearExpired(days = 7) {
    const limit = Date.now() - days * 24 * 3600 * 1000;
    const keys = await storage.keys();
    await Promise.all(keys.map(async (k: string) => {
        const item = await storage.getItem(k);
        if (item) {
            if ((item.expiresAt && item.expiresAt < Date.now()) || 
                (item.lastAccess && item.lastAccess < limit)) {
                await storage.removeItem(k);
            }
        }
    }));
}

