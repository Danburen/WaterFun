package org.waterwood.waterfunservicecore.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.waterwood.common.cache.RedisHelperInterface;
import org.waterwood.utils.JsonUtil;
import org.waterwood.utils.StringUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class RedisHelper implements RedisHelperInterface {
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
        //log.info("redis set value. key: {}, value: {}",getCurrentKey(key), value);
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
        redisTemplate.opsForHash().delete(getCurrentKey(key), (Object[]) fields);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return JsonUtil.fromJson(redisTemplate.opsForValue().get(getCurrentKey(key)), clazz);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(getCurrentKey(key));
    }

    public <T> boolean validateAndRemove(String pKey, T value) {
        String stored = getValue(pKey);
        log.info("key: {}, stored: {}, value: {}, equal:{}",
                getCurrentKey(pKey) ,
                StringUtil.noNullStringArray(stored),
                value,
                stored != null && stored.equals(value));
        if (stored == null || !stored.equals(value)) {
            return false;
        }
        del(pKey);
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
