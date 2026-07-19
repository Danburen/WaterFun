package org.waterwood.waterfunservicecore.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.waterwood.utils.JsonUtil;
import org.waterwood.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public void del(List<String> redisKeys) {
        if (redisKeys == null || redisKeys.isEmpty()) {
            return;
        }
        redisTemplate.delete(redisKeys);
    }

    public <T> void set(String key, T value, Duration expire) {
        redisTemplate.opsForValue().set(key, JsonUtil.toJson(value), expire);
    }

    public void set(String key, String value, Duration expire) {
        //log.info("redis set value. key: {}, value: {}", key, value);
        redisTemplate.opsForValue().set(key, value, expire);
    }

    @Override
    public void hashDel(String key, String... fields) {
        if (fields == null || fields.length == 0) return;
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    @Override
    public void hashSetMap(String key, Map<String, String> map, Duration expire) {
        if (map == null) return;
        redisTemplate.delete(key);
        if (!map.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, map);
        }
        redisTemplate.expire(key, expire);
    }

    @Override
    public void hashSetMap(String key, Map<String, String> map) {
        if (map == null) return;
        redisTemplate.delete(key);
        if (!map.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, map);
        }
    }

    @Override
    public void hashSetAll(String key, Map<String, String> map) {
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            redisTemplate.opsForHash().putIfAbsent(key, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void hashSet(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public String hashGet(String key, String field) {
        return (String) redisTemplate.opsForHash().get(key, field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> hashGetAll(String key) {
        return (Map<String, String>) (Map<?, ?>) redisTemplate.opsForHash().entries(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> hashKeys(String key) {
        return (Set<String>) (Set<?>) redisTemplate.opsForHash().keys(key);
    }

    @Override
    public void setAdd(String key, String... values) {
            redisTemplate.opsForSet().add(key, values);
    }
    @Override
    public void setRemove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    @Override
    public void setRemove(String key, Collection<String> members) {
        if(members != null && !members.isEmpty()) {
            redisTemplate.opsForSet().remove(key, members.toArray());
        }
    }

    @Override
    public Set<String> setMembers(String key) {
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
//        log.info("key: {}, stored: {}, value: {}, equal:{}",
//                pKey ,
//                StringUtil.noNullStringArray(stored),
//                value,
//                stored != null && stored.equals(value));
        if (stored == null || !stored.equals(value)) {
            return false;
        }
        del(pKey);
        return true;
    }

    @Override
    public Long increment(String key, Duration expire) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, expire);
        }
        return count;
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    @Override
    public List<String> multiGet(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        return values != null ? values : Collections.nCopies(keys.size(), null);
    }

    @Override
    public List<Long> multiGetExpire(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (String key : keys) {
                        connection.keyCommands().ttl(key.getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                }).stream()
                .map(result -> result == null ? -2L : ((Number) result).longValue())
                .collect(Collectors.toList());
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
        return multiGet(keys).stream().map(Objects::nonNull).toList();
    }

    @Override
    public String getAndDel(String redisKey) {
        String val =  getValue(redisKey);
        del(redisKey);
        return val;
    }

    @Override
    public void mset(Map<String, String> toCache, Duration dur) {
        if (toCache == null || toCache.isEmpty()) {
            return;
        }
        long seconds = Math.max(1, dur.getSeconds());

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            toCache.forEach((key, value) -> {
                if (key == null || value == null) {
                    return; // skip
                }
                connection.stringCommands().setEx(
                        key.getBytes(StandardCharsets.UTF_8),
                        seconds,
                        value.getBytes(StandardCharsets.UTF_8)
                );
            });
            return null;
        });
    }

    @Override
    public Map<String, String> hGetAllAndDel(String key) {
        Map<String, String> res = Optional.ofNullable(hashGetAll(key)).orElse(Collections.emptyMap());
        del(key);
        return res;
    }
}
