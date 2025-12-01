package org.waterwood.common.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

public interface RedisHelperInterface {
    void setKeyPrefix(String redisKeyPrefix);

    void del(String key);

    <T> void set(String key, T value, Duration expire);

    void set(String key, String value, Duration expire);

    <T> void hSet(String key, String field, T value);

    <T> T hGet(String key, String field, Class<T> clazz);

    Map<Object, Object> hGetAll(String key);

    Set<Object> hKeys(String key);

    void hDel(String key, String... fields);

    <T> T getValue(String key, Class<T> clazz);

    String getValue(String key);

    <T> boolean validateAndRemove(String key, T value);

    Long getExpire(String key);

    String buildKeys(String... keys);

    String getCurrentKey(String... keys);
}
