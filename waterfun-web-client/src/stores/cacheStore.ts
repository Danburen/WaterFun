import { defineStore } from 'pinia'
import { getMemory, setMemory, revokeMemory } from '@waterfun/web-core/src/cache/imgMemoryLRU'
import { getIDB, setIDB, clearExpired } from '@waterfun/web-core/src/cache/imgIDB'
import type CacheItem from '@waterfun/web-core/src/cache/types'

export const useCacheStore = defineStore('cacheStore',()=>{
    const map = new Map<string, string>()
    const inflight = new Map<string, Promise<string>>()
    clearExpired()
    
    const updateCacheItem = (path: string, item: CacheItem) => {
        setMemory(path, item.presignedUrl, item.expiresAt)
        setIDB(path, item.presignedUrl, item.expiresAt)
    }

    const doLoad = async (path: string, fetchFn: (url: string) => Promise<CacheItem>) => {
        // 1. memory
        const mem = getMemory(path)
        if (mem) return mem.presignedUrl
        // 2. IDB
        const idb = await getIDB(path)
        if (idb) {
            setMemory(path, idb.presignedUrl, idb.expiresAt) 
            return idb.presignedUrl
        }
        // 3. network
        const fresh = await fetchFn(path)
        setMemory(path, fresh.presignedUrl, fresh.expiresAt)
        await setIDB(path, fresh.presignedUrl, fresh.expiresAt)
        return fresh.presignedUrl
    }

    const load = (path: string, fetchFn: (url: string) => Promise<CacheItem>): Promise<string> => {
        if (map.has(path)) return Promise.resolve(map.get(path)!)
        if (inflight.has(path)) return inflight.get(path)!
        const p = doLoad(path, fetchFn)
        .then(url => {
            map.set(path, url)
            return url
        })
        .finally(() => inflight.delete(path))
        inflight.set(path, p)
    return p
  }

    const revoke = (path: string) => {
      const item = map.get(path)
      if (item) {
        URL.revokeObjectURL(item)   
        revokeMemory(path)
        map.delete(path)
      }
    }

    return {
        load,
        updateCacheItem,
        revoke
    }
})