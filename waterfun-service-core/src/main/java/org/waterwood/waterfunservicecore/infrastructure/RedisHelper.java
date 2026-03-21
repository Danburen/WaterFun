package org.waterwood.waterfunservicecore.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.waterwood.utils.JsonUtil;
import org.waterwood.utils.StringUtil;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class RedisHelper implements RedisHelperHolder {
    private final StringRedisTemplate redisTemplate;

    protected RedisHelper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void del(String key) {
        redisTemplate.delete(key);
    }

    public <T> void set(String key, T value, Duration expire) {
        redisTemplate.opsForValue().set(key, JsonUtil.toJson(value), expire);
    }

    public void set(String key, String value, Duration expire) {
        //log.info("redis set value. key: {}, value: {}", key, value);
        redisTemplate.opsForValue().set(key, value, expire);
    }

    public <T> void hSet(String key, String field, T value) {
        redisTemplate.opsForHash().put(key, field, JsonUtil.toJson(value));
    }
    public <T> T hGet(String key, String field, Class<T> clazz) {
        String json = (String) redisTemplate.opsForHash().get(key, field);
        return JsonUtil.fromJson(json, clazz);
    }

    public Map<Object,Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Set<Object> hKeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    public void hDel(String key, String... fields) {
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    @Override
    public void sAdd(String key, String... values) {
            redisTemplate.opsForSet().add(key, values);
    }
    @Override
    public void sRem(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    @Override
    public void sRem(String key, Collection<String> members) {
        if(members != null && !members.isEmpty()) {
            redisTemplate.opsForSet().remove(key, members.toArray());
        }
    }

    @Override
    public Set<String> sMem(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return JsonUtil.fromJson(redisTemplate.opsForValue().get(key), clazz);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> boolean validateAndRemove(String pKey, T value) {
        String stored = getValue(pKey);
        log.info("key: {}, stored: {}, value: {}, equal:{}",
                pKey ,
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
        return redisTemplate.getExpire(key);
    }

    @Override
    public List<String> mget(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.nCopies(keys.size(), null);
        }
        return values;
    }

    @Override
    public Cursor<String> scan(ScanOptions options) {
        return redisTemplate.scan(options);
    }

    @Override
    public boolean hasKey(String ket) {
        return redisTemplate.hasKey(ket);
    }

    @Override
    public List<Boolean> hasKeys(List<String> keys) {
        return mget(keys).stream().map(Objects::nonNull).toList();
    }
}
