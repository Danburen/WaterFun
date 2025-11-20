package org.waterwood.waterfunadminservice.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.waterwood.utils.JsonUtil;
import org.waterwood.utils.StringUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RedisHelper {
    private final StringRedisTemplate redisTemplate;
    private String redisKeyPrefix="";

    protected RedisHelper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix.replace(":", "");
    }

    public void del(String key) {
        redisTemplate.delete(getCurrentKey(key));
    }


    public <T> void set(String key, T value, Duration expire) {
        redisTemplate.opsForValue().set(getCurrentKey(key), JsonUtil.toJson(value), expire);
    }

    public void set(String key, String value, Duration expire) {
        redisTemplate.opsForValue().set(getCurrentKey(key), value, expire);
    }

    public <T> void hSet(String key, String field, T value) {
        redisTemplate.opsForHash().put(getCurrentKey(key), field, JsonUtil.toJson(value));
    }
    public <T> T hGet(String key, String field, Class<T> clazz) {
        String json = (String) redisTemplate.opsForHash().get(key, field);
        return JsonUtil.fromJson(json, clazz);
    }

    public Map<Object,Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(getCurrentKey(key));
    }

    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(getCurrentKey(key));
    }

    public void hDel(String key, String... fields) {
        redisTemplate.opsForHash().delete(getCurrentKey(key), List.of(fields));
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return JsonUtil.fromJson(redisTemplate.opsForValue().get(getCurrentKey(key)), clazz);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(getCurrentKey(key));
    }

    public <T> boolean validateAndRemove(String key, T value) {
        String stored = getValue(key);
        // log.info("stored: {}, value: {}, equal:{}", stored, value, stored.equals(value));
        if (stored == null || !stored.equals(value)) {
            return false;
        }
        del(key);
        return true;
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(getCurrentKey(key));
    }

    /**
     * Build redis key path by {@code delimiter ':'}
     * @param keys redis keys
     * @return joint redis key prefix
     */
    public String buildKeys(String... keys) {
        return String.join(":", StringUtil.noNullStringArray(keys));
    }
    public String getCurrentKey(String... keys) {
        if(StringUtil.isBlank(redisKeyPrefix)){
            return String.join(":", keys);
        }
        return redisKeyPrefix.concat(":").concat(String.join(":", keys));
    }
}
