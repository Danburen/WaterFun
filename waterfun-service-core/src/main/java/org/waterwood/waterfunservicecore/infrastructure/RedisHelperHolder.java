package org.waterwood.waterfunservicecore.infrastructure;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisHelperHolder {

    void del(String key);
    /**
     * batch del by keys
     * @param redisKeys redis keys
     */
    void del(List<String> redisKeys);

    <T> void set(String key, T value, Duration expire);

    void set(String key, String value, Duration expire);

    void hashSet(String key, String field, String value);

    String hashGet(String key, String field);

    void hashSetMap(String key, Map<String, String> map, Duration expire);

    void hashSetMap(String key, Map<String, String> map);

    void hashSetAll(String key, Map<String, String> map);

    Map<String, String> hashGetAll(String key);
    Set<String> hashKeys(String key);

    void hashDel(String key, String... fields);
    void setAdd(String key, String... values);
    void setRemove(String key, Object... values);
    void setRemove(String key, Collection<String> members);
    Set<String> setMembers(String key);

    <T> T getValue(String key, Class<T> clazz);

    String getValue(String key);

    <T> boolean validateAndRemove(String key, T value);

    Long getExpire(String key);

    /**
     * Redis pipeline multiGet
     *
     * @param keys redis keys
     * @return redis value, same size of input
     */
    List<String> multiGet(List<String> keys);

    /**
     * Batch get TTL (time-to-live) for multiple keys using pipeline.
     *
     * @param keys list of keys to query
     * @return list of TTL in seconds, aligned with input order.
     *         -1 if key has no expiration, -2 if key does not exist
     */
    List<Long> multiGetExpire(List<String> keys);

    Cursor<String> scan(ScanOptions options);

    boolean hasKey(String key);
    List<Boolean> hasKeys(List<String> keys);

    /**
     * Get a value and then delete it from redis.
     * @param redisKey target redis key
     */
    String getAndDel(String redisKey);

    /**
     * Redis multiply set
     * @param toCache key-value to save
     * @param dur duration of key-values.
     */
    void mset(Map<String, String> toCache, Duration dur);

    Map<String, String> hGetAllAndDel(String s);
}
